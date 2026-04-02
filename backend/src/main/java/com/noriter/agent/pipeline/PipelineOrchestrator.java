package com.noriter.agent.pipeline;

import com.noriter.agent.core.*;
import com.noriter.agent.impl.*;
import com.noriter.domain.Project;
import com.noriter.domain.Stage;
import com.noriter.domain.enums.*;
import com.noriter.infrastructure.sse.SseEmitterService;
import com.noriter.infrastructure.sse.SseEvent;
import com.noriter.infrastructure.storage.FileStorageService;
import com.noriter.repository.ProjectRepository;
import com.noriter.repository.StageRepository;
import com.noriter.service.ArtifactService;
import com.noriter.service.AuditService;
import com.noriter.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 파이프라인 오케스트레이터
 * 참조: 03_아키텍처 §3.4 PipelineOrchestrator
 *
 * 파이프라인 7단계를 순차/병렬로 제어한다.
 * STAGE 1(기획) → 2(CTO) → 3(디자인) → 4(구현, 병렬) → 5(QA) → 6(디버깅, 조건부) → 7(출시)
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class PipelineOrchestrator {

    private final ProjectRepository projectRepository;
    private final StageRepository stageRepository;
    private final ArtifactService artifactService;
    private final FileStorageService fileStorageService;
    private final StageExecutor stageExecutor;
    private final CodeMerger codeMerger;
    private final AuditService auditService;
    private final LogService logService;
    private final SseEmitterService sseEmitterService;

    // 에이전트 구현체들 (Spring Bean으로 주입)
    private final PlanningAgent planningAgent;
    private final CtoAgent ctoAgent;
    private final DesignAgent designAgent;
    private final FrontendAgent frontendAgent;
    private final BackendAgent backendAgent;
    private final QaAgent qaAgent;

    /**
     * 파이프라인 시작
     * 참조: 03_아키텍처 §3.4 startPipeline()
     */
    @Async
    public void startPipeline(String projectId) {
        log.info("[파이프라인] ===== 시작 ===== projectId={}", projectId);

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            log.error("[파이프라인] 프로젝트를 찾을 수 없음 - projectId={}", projectId);
            return;
        }

        project.updateStatus(ProjectStatus.IN_PROGRESS);
        projectRepository.save(project);
        auditService.log(AuditEventType.PROJECT_STATUS_CHANGED, projectId,
                "파이프라인 시작 (IN_PROGRESS)", null);

        List<Stage> stages = stageRepository.findByProjectIdOrderByStageOrderAsc(projectId);
        Map<String, String> artifacts = new HashMap<>();  // 산출물 누적

        try {
            // STAGE 1: 기획
            AgentResult planResult = executeStage(project, stages, StageType.PLANNING,
                    planningAgent, artifacts);
            if (!handleResult(project, planResult, stages, StageType.PLANNING, artifacts)) return;

            // STAGE 2: CTO 검토
            AgentResult ctoResult = executeStage(project, stages, StageType.ARCHITECTURE,
                    ctoAgent, artifacts);
            if (!handleResult(project, ctoResult, stages, StageType.ARCHITECTURE, artifacts)) return;

            // STAGE 3: 디자인
            AgentResult designResult = executeStage(project, stages, StageType.DESIGN,
                    designAgent, artifacts);
            if (!handleResult(project, designResult, stages, StageType.DESIGN, artifacts)) return;

            // STAGE 4: 구현 (프론트+백엔드 순차 — TODO: 병렬 전환)
            log.info("[파이프라인] STAGE 4 구현 진입 - projectId={}", projectId);
            project.updateProgress(57, StageType.IMPLEMENTATION);
            projectRepository.save(project);

            AgentResult frontResult = executeStage(project, stages, StageType.IMPLEMENTATION,
                    frontendAgent, artifacts);
            if (frontResult.getStatus() == AgentResult.Status.FAILED) {
                handlePipelineFailure(project, "프론트엔드 구현 실패: " + frontResult.getErrorMessage());
                return;
            }

            AgentResult backResult = executeStage(project, stages, StageType.IMPLEMENTATION,
                    backendAgent, artifacts);
            if (backResult.getStatus() == AgentResult.Status.FAILED) {
                handlePipelineFailure(project, "백엔드 구현 실패: " + backResult.getErrorMessage());
                return;
            }

            // 코드 병합 (08_프롬프트 §12)
            mergeCode(projectId, frontResult, backResult, artifacts);

            Stage implStage = findStage(stages, StageType.IMPLEMENTATION);
            if (implStage != null) {
                implStage.complete(null);
                stageRepository.save(implStage);
            }

            // STAGE 5: QA
            AgentResult qaResult = executeStage(project, stages, StageType.QA,
                    qaAgent, artifacts);

            // QA 결과 처리 — PASS면 출시, FAIL이면 디버깅 루프
            if (qaResult.getStatus() == AgentResult.Status.NEEDS_REVIEW) {
                handleQaFailure(project, stages, artifacts, qaResult);
            } else if (qaResult.getStatus() == AgentResult.Status.SUCCESS) {
                handleResult(project, qaResult, stages, StageType.QA, artifacts);
                completeRelease(project, stages);
            } else {
                handlePipelineFailure(project, "QA 실패: " + qaResult.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("[파이프라인] 예상치 못한 오류 - projectId={}, error={}", projectId, e.getMessage(), e);
            handlePipelineFailure(project, "파이프라인 오류: " + e.getMessage());
        }
    }

    /**
     * QA 실패 → 디버깅 루프 (최대 3회)
     * 참조: 03_아키텍처 §4.3, NT-AGT-005
     */
    private void handleQaFailure(Project project, List<Stage> stages,
                                  Map<String, String> artifacts, AgentResult qaResult) {
        String projectId = project.getId();
        int maxAttempts = project.getMaxDebugAttempts();

        while (project.getDebugAttempts() < maxAttempts) {
            project.incrementDebugAttempts();
            projectRepository.save(project);

            log.info("[파이프라인] 디버깅 루프 진입 - projectId={}, 시도 {}/{}",
                    projectId, project.getDebugAttempts(), maxAttempts);

            logService.createLog(projectId, LogLevel.WARN, AgentRole.QA, StageType.QA,
                    String.format("QA 테스트 실패 - 디버깅 시도 %d/%d회", project.getDebugAttempts(), maxAttempts));

            // TODO: CTO 디버깅 배정 (PROMPT-CTO-DBG)
            // TODO: 담당 에이전트 수정 (PROMPT-FRONT-FIX / PROMPT-BACK-FIX)
            // TODO: QA 재테스트 (PROMPT-QA-RETEST)

            // 현재는 디버깅 미구현으로 실패 처리
            log.warn("[파이프라인] 디버깅 아직 미구현 - projectId={}, FAILED 처리", projectId);
            handlePipelineFailure(project, "디버깅 미구현 (구현 예정)");
            return;
        }

        log.warn("[파이프라인] 디버깅 최대 횟수 초과 - projectId={}, {}회 시도 후 FAILED",
                projectId, maxAttempts);
        handlePipelineFailure(project, String.format("디버깅 %d회 시도 후 실패", maxAttempts));
    }

    /**
     * STAGE 7: 출시 처리
     */
    private void completeRelease(Project project, List<Stage> stages) {
        String projectId = project.getId();
        log.info("[파이프라인] STAGE 7 출시 - projectId={}", projectId);

        Stage releaseStage = findStage(stages, StageType.RELEASE);
        if (releaseStage != null) {
            releaseStage.start();
            releaseStage.complete(null);
            stageRepository.save(releaseStage);
        }

        project.updateStatus(ProjectStatus.COMPLETED);
        project.updateProgress(100, StageType.RELEASE);
        projectRepository.save(project);

        auditService.log(AuditEventType.PROJECT_STATUS_CHANGED, projectId,
                "파이프라인 완료 (COMPLETED)", null);

        logService.createLog(projectId, LogLevel.INFO, AgentRole.SYSTEM, StageType.RELEASE,
                "게임 생성이 완료되었습니다! 미리보기와 다운로드가 가능합니다.");

        sseEmitterService.sendEvent(projectId, SseEvent.complete(
                String.format("{\"projectId\":\"%s\",\"status\":\"COMPLETED\"}", projectId)));
        sseEmitterService.completeAll(projectId);

        log.info("[파이프라인] ===== 완료 ===== projectId={}", projectId);
    }

    private AgentResult executeStage(Project project, List<Stage> stages,
                                      StageType stageType, BaseAgent agent,
                                      Map<String, String> artifacts) {
        String projectId = project.getId();
        log.info("[파이프라인] {} 스테이지 진입 - projectId={}", stageType, projectId);

        int progress = calculateProgress(stageType);
        project.updateProgress(progress, stageType);
        projectRepository.save(project);

        Stage stage = findStage(stages, stageType);
        if (stage == null && stageType != StageType.IMPLEMENTATION) {
            log.error("[파이프라인] 스테이지를 찾을 수 없음 - projectId={}, type={}", projectId, stageType);
            return AgentResult.failed("스테이지를 찾을 수 없음: " + stageType);
        }

        AgentContext context = AgentContext.builder()
                .projectId(projectId)
                .stageType(stageType)
                .requirement(project.getRequirement())
                .genre(project.getGenre() != null ? project.getGenre().name() : null)
                .previousArtifacts(new HashMap<>(artifacts))
                .debugAttempt(project.getDebugAttempts())
                .build();

        return stageExecutor.executeAgent(agent, context, stage != null ? stage : stages.get(3));
    }

    private boolean handleResult(Project project, AgentResult result, List<Stage> stages,
                                  StageType stageType, Map<String, String> artifacts) {
        if (result.getStatus() == AgentResult.Status.FAILED) {
            handlePipelineFailure(project, stageType + " 실패: " + result.getErrorMessage());
            return false;
        }

        // 산출물 저장
        if (result.getArtifacts() != null) {
            for (Map.Entry<String, String> entry : result.getArtifacts().entrySet()) {
                artifacts.put(entry.getKey(), entry.getValue());
                String filePath = fileStorageService.saveArtifact(project.getId(), entry.getKey(), entry.getValue());
                log.debug("[파이프라인] 산출물 저장 - projectId={}, file={}", project.getId(), entry.getKey());
            }
        }

        Stage stage = findStage(stages, stageType);
        if (stage != null && stage.getStatus() != StageStatus.COMPLETED) {
            stage.complete(null);
            stageRepository.save(stage);
        }

        return true;
    }

    private void handlePipelineFailure(Project project, String reason) {
        log.error("[파이프라인] 실패 처리 - projectId={}, reason={}", project.getId(), reason);

        project.updateStatus(ProjectStatus.FAILED);
        projectRepository.save(project);

        auditService.log(AuditEventType.PROJECT_STATUS_CHANGED, project.getId(),
                "파이프라인 실패 (FAILED): " + reason, null);

        sseEmitterService.sendEvent(project.getId(), SseEvent.error(
                String.format("{\"message\":\"%s\"}", reason)));
        sseEmitterService.completeAll(project.getId());
    }

    private void mergeCode(String projectId, AgentResult frontResult, AgentResult backResult,
                           Map<String, String> artifacts) {
        log.info("[파이프라인] 코드 병합 시작 - projectId={}", projectId);

        String frontRender = frontResult.getArtifacts() != null
                ? frontResult.getArtifacts().get("gameJsRenderSection") : "";
        String backLogic = backResult.getArtifacts() != null
                ? backResult.getArtifacts().get("gameJsLogicSection") : "";

        String mergedGameJs = codeMerger.mergeGameJs(backLogic, frontRender);
        fileStorageService.saveGameFile(projectId, "game.js", mergedGameJs);
        artifacts.put("game.js", mergedGameJs);

        // 프론트 산출물 파일 저장 (index.html, style.css)
        if (frontResult.getArtifacts() != null) {
            for (Map.Entry<String, String> entry : frontResult.getArtifacts().entrySet()) {
                if (entry.getKey().endsWith(".html") || entry.getKey().endsWith(".css")) {
                    fileStorageService.saveGameFile(projectId, entry.getKey(), entry.getValue());
                    artifacts.put(entry.getKey(), entry.getValue());
                }
            }
        }

        log.info("[파이프라인] 코드 병합 완료 - projectId={}", projectId);
    }

    private Stage findStage(List<Stage> stages, StageType type) {
        return stages.stream()
                .filter(s -> s.getType() == type)
                .findFirst()
                .orElse(null);
    }

    private int calculateProgress(StageType stageType) {
        return switch (stageType) {
            case PLANNING -> 14;
            case ARCHITECTURE -> 28;
            case DESIGN -> 42;
            case IMPLEMENTATION -> 57;
            case QA -> 71;
            case DEBUG -> 80;
            case RELEASE -> 100;
        };
    }
}
