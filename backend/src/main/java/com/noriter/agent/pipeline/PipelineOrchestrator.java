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
     * 실패 지점부터 재시도 — 기존 산출물 재활용
     */
    @Async
    public void resumePipeline(String projectId, StageType fromStage) {
        log.info("[파이프라인] ===== 재시도 ===== projectId={}, fromStage={}", projectId, fromStage);

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return;

        project.updateStatus(ProjectStatus.IN_PROGRESS);
        projectRepository.save(project);
        auditService.log(AuditEventType.PROJECT_STATUS_CHANGED, projectId,
                String.format("파이프라인 재시도 (%s부터)", fromStage), null);

        List<Stage> stages = stageRepository.findByProjectIdOrderByStageOrderAsc(projectId);

        // 기존 산출물 로드
        Map<String, String> artifacts = loadExistingArtifacts(projectId);
        log.info("[파이프라인] 기존 산출물 로드 완료 - {}개: {}", artifacts.size(), artifacts.keySet());

        int fromOrder = getStageOrder(fromStage);

        try {
            // STAGE 1: 기획 (fromOrder 이전이면 건너뜀)
            if (fromOrder <= 1) {
                AgentResult planResult = executeStage(project, stages, StageType.PLANNING, planningAgent, artifacts);
                if (!handleResult(project, planResult, stages, StageType.PLANNING, artifacts)) return;
            }

            if (fromOrder <= 2) {
                AgentResult ctoResult = executeStage(project, stages, StageType.ARCHITECTURE, ctoAgent, artifacts);
                if (!handleResult(project, ctoResult, stages, StageType.ARCHITECTURE, artifacts)) return;
            }

            if (fromOrder <= 3) {
                AgentResult designResult = executeStage(project, stages, StageType.DESIGN, designAgent, artifacts);
                if (!handleResult(project, designResult, stages, StageType.DESIGN, artifacts)) return;
            }

            if (fromOrder <= 4) {
                project.updateProgress(57, StageType.IMPLEMENTATION);
                projectRepository.save(project);

                AgentResult frontResult = executeStage(project, stages, StageType.IMPLEMENTATION, frontendAgent, artifacts);
                if (frontResult.getStatus() == AgentResult.Status.FAILED) {
                    handlePipelineFailure(project, "프론트엔드 구현 실패: " + frontResult.getErrorMessage());
                    return;
                }

                AgentResult backResult = executeStage(project, stages, StageType.IMPLEMENTATION, backendAgent, artifacts);
                if (backResult.getStatus() == AgentResult.Status.FAILED) {
                    handlePipelineFailure(project, "백엔드 구현 실패: " + backResult.getErrorMessage());
                    return;
                }

                mergeCode(projectId, frontResult, backResult, artifacts);
                Stage implStage = findStage(stages, StageType.IMPLEMENTATION);
                if (implStage != null) { implStage.complete(null); stageRepository.save(implStage); }
            }

            if (fromOrder <= 5) {
                AgentResult qaResult = executeStage(project, stages, StageType.QA, qaAgent, artifacts);
                if (qaResult.getStatus() == AgentResult.Status.NEEDS_REVIEW) {
                    handleQaFailure(project, stages, artifacts, qaResult);
                } else if (qaResult.getStatus() == AgentResult.Status.SUCCESS) {
                    handleResult(project, qaResult, stages, StageType.QA, artifacts);
                    completeRelease(project, stages);
                } else {
                    handlePipelineFailure(project, "QA 실패: " + qaResult.getErrorMessage());
                }
            }

        } catch (Exception e) {
            log.error("[파이프라인] 재시도 중 오류 - projectId={}, error={}", projectId, e.getMessage(), e);
            handlePipelineFailure(project, "파이프라인 재시도 오류: " + e.getMessage());
        }
    }

    /** 기존 산출물 파일 로드 */
    private Map<String, String> loadExistingArtifacts(String projectId) {
        Map<String, String> artifacts = new HashMap<>();
        String[] artifactFiles = {"plan.json", "architecture.json", "design.json"};
        for (String file : artifactFiles) {
            try {
                String content = fileStorageService.readArtifact(projectId, file);
                if (content != null && !content.isBlank()) {
                    artifacts.put(file, content);
                }
            } catch (Exception e) {
                log.debug("[파이프라인] 산출물 없음 - {}", file);
            }
        }
        // 게임 파일
        String[] gameFiles = {"index.html", "style.css", "game.js"};
        for (String file : gameFiles) {
            try {
                String content = fileStorageService.readGameFile(projectId, file);
                if (content != null && !content.isBlank()) {
                    artifacts.put(file, content);
                }
            } catch (Exception e) {
                log.debug("[파이프라인] 게임 파일 없음 - {}", file);
            }
        }
        return artifacts;
    }

    private int getStageOrder(StageType type) {
        return switch (type) {
            case PLANNING -> 1;
            case ARCHITECTURE -> 2;
            case DESIGN -> 3;
            case IMPLEMENTATION -> 4;
            case QA -> 5;
            case DEBUG -> 5;
            case RELEASE -> 6;
        };
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

            int attempt = project.getDebugAttempts();
            log.info("[파이프라인] 디버깅 루프 #{} - projectId={}", attempt, projectId);

            logService.createLog(projectId, LogLevel.WARN, AgentRole.QA, StageType.QA,
                    String.format("QA 테스트 실패 - 디버깅 시도 %d/%d회", attempt, maxAttempts));

            try {
                // 1) CTO가 버그 분석 + 수정 지시
                project.updateProgress(75 + (attempt * 2), StageType.DEBUG);
                projectRepository.save(project);
                sseEmitterService.sendEvent(projectId, SseEvent.stageUpdate(
                        String.format("{\"stage\":\"DEBUG\",\"status\":\"CTO_ANALYZING\",\"attempt\":%d}", attempt)));

                logService.createLog(projectId, LogLevel.INFO, AgentRole.CTO, StageType.DEBUG,
                        String.format("CTO가 버그를 분석하고 있어요... (시도 %d/%d)", attempt, maxAttempts));

                String bugReport = artifacts.getOrDefault("test-report.json", "");
                AgentContext debugContext = AgentContext.builder()
                        .projectId(projectId)
                        .stageType(StageType.DEBUG)
                        .requirement(project.getRequirement())
                        .previousArtifacts(new HashMap<>(artifacts))
                        .bugReport(bugReport)
                        .debugAttempt(attempt)
                        .build();

                AgentResult ctoDebugResult = ctoAgent.executeDebug(debugContext);
                if (ctoDebugResult.getStatus() == AgentResult.Status.FAILED) {
                    handlePipelineFailure(project, "CTO 디버그 분석 실패");
                    return;
                }
                String ctoInstruction = ctoDebugResult.getArtifacts().getOrDefault("debug-instruction.json", "");

                // 2) Frontend/Backend 수정
                project.updateProgress(80 + (attempt * 2), StageType.DEBUG);
                projectRepository.save(project);
                sseEmitterService.sendEvent(projectId, SseEvent.stageUpdate(
                        String.format("{\"stage\":\"DEBUG\",\"status\":\"FIXING\",\"attempt\":%d}", attempt)));

                logService.createLog(projectId, LogLevel.INFO, AgentRole.FRONTEND, StageType.DEBUG,
                        "발견된 버그를 수정하고 있어요...");

                AgentContext fixContext = AgentContext.builder()
                        .projectId(projectId)
                        .stageType(StageType.DEBUG)
                        .requirement(project.getRequirement())
                        .previousArtifacts(new HashMap<>(artifacts))
                        .ctoInstruction(ctoInstruction)
                        .bugReport(bugReport)
                        .debugAttempt(attempt)
                        .build();

                AgentResult frontFixResult = frontendAgent.executeFix(fixContext);
                artifacts.putAll(frontFixResult.getArtifacts());

                AgentResult backFixResult = backendAgent.executeFix(fixContext);
                artifacts.putAll(backFixResult.getArtifacts());

                // 코드 재병합
                String backLogic = artifacts.getOrDefault("gameJsLogicSection", "");
                String frontRender = artifacts.getOrDefault("gameJsRenderSection", "");
                String mergedJs = codeMerger.mergeGameJs(backLogic, frontRender);
                artifacts.put("game.js", mergedJs);
                fileStorageService.saveGameFile(projectId, "game.js", mergedJs);

                // 3) QA 재테스트
                project.updateProgress(85 + (attempt * 2), StageType.DEBUG);
                projectRepository.save(project);
                sseEmitterService.sendEvent(projectId, SseEvent.stageUpdate(
                        String.format("{\"stage\":\"DEBUG\",\"status\":\"RETESTING\",\"attempt\":%d}", attempt)));

                logService.createLog(projectId, LogLevel.INFO, AgentRole.QA, StageType.DEBUG,
                        "수정된 코드를 다시 검증하고 있어요...");

                AgentContext retestContext = AgentContext.builder()
                        .projectId(projectId)
                        .stageType(StageType.QA)
                        .requirement(project.getRequirement())
                        .previousArtifacts(new HashMap<>(artifacts))
                        .debugAttempt(attempt)
                        .build();

                AgentResult retestResult = qaAgent.execute(retestContext);

                if (retestResult.getStatus() == AgentResult.Status.SUCCESS) {
                    log.info("[파이프라인] 디버깅 성공! - projectId={}, {}회 만에 통과", projectId, attempt);
                    artifacts.putAll(retestResult.getArtifacts());
                    completeRelease(project, stages);
                    return;
                }

                // 재테스트도 실패 → 다음 루프
                artifacts.putAll(retestResult.getArtifacts());
                log.warn("[파이프라인] 디버깅 #{} 재테스트 실패 - projectId={}", attempt, projectId);

            } catch (Exception e) {
                log.error("[파이프라인] 디버깅 #{} 중 예외 - projectId={}, error={}", attempt, projectId, e.getMessage());
                handlePipelineFailure(project, "디버깅 중 오류: " + e.getMessage());
                return;
            }
        }

        log.warn("[파이프라인] 디버깅 최대 횟수 초과 - projectId={}, {}회 시도 후 FAILED",
                projectId, maxAttempts);
        handlePipelineFailure(project, String.format("디버깅 %d회 시도 후에도 QA 통과 실패", maxAttempts));
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
