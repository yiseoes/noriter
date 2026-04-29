package com.noriter.agent.pipeline;

import com.noriter.agent.core.AgentContext;
import com.noriter.agent.core.AgentResult;
import com.noriter.agent.core.BaseAgent;
import com.noriter.domain.Stage;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.AuditEventType;
import com.noriter.domain.enums.StageType;
import com.noriter.service.AuditService;
import com.noriter.service.LogService;
import com.noriter.service.TokenUsageService;
import com.noriter.domain.enums.LogLevel;
import com.noriter.infrastructure.sse.SseEmitterService;
import com.noriter.infrastructure.sse.SseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * 개별 스테이지 실행기
 * 참조: 03_아키텍처 §3.4 PipelineOrchestrator의 스테이지별 실행 담당
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class StageExecutor {

    private final LogService logService;
    private final AuditService auditService;
    private final TokenUsageService tokenUsageService;
    private final SseEmitterService sseEmitterService;

    /**
     * 에이전트를 실행하고 결과를 처리한다
     */
    public AgentResult executeAgent(BaseAgent agent, AgentContext context, Stage stage) {
        String projectId = context.getProjectId();
        StageType stageType = context.getStageType();
        AgentRole role = agent.getRole();

        log.info("[스테이지 실행] 에이전트 시작 - projectId={}, stage={}, agent={}",
                projectId, stageType, role);

        // 스테이지 시작
        stage.start();
        auditService.log(AuditEventType.STAGE_STARTED, projectId,
                String.format("%s 스테이지 시작 (%s)", stageType, role), null);

        // SSE: 스테이지 상태 변경 알림
        sseEmitterService.sendEvent(projectId, SseEvent.stageUpdate(
                String.format("{\"stage\":\"%s\",\"status\":\"IN_PROGRESS\"}", stageType)));

        // 로그 기록: 에이전트 작업 시작
        logService.createLog(projectId, LogLevel.INFO, role, stageType,
                String.format("%s, 작업 시작할게요!", getAgentDisplayName(role)));

        try {
            // 에이전트 실행
            AgentResult result = agent.execute(context);

            // 토큰 사용량 기록
            if (result.getInputTokens() > 0 || result.getOutputTokens() > 0) {
                tokenUsageService.record(projectId, role, stageType,
                        result.getInputTokens(), result.getOutputTokens());
            }

            if (result.getStatus() == AgentResult.Status.SUCCESS) {
                log.info("[스테이지 실행] 에이전트 성공 - projectId={}, stage={}, agent={}",
                        projectId, stageType, role);

                // 에이전트가 전달한 상세 메시지 로그
                if (result.getMessage() != null && !result.getMessage().isBlank()) {
                    logService.createLog(projectId, LogLevel.AGENT, role, stageType, result.getMessage());
                }

                // 산출물 목록 로그
                if (result.getArtifacts() != null && !result.getArtifacts().isEmpty()) {
                    String artifactNames = String.join(", ", result.getArtifacts().keySet());
                    logService.createLog(projectId, LogLevel.INFO, role, stageType,
                            String.format("산출물 생성 완료: %s", artifactNames));
                }

                // 토큰 사용량 로그
                if (result.getInputTokens() > 0) {
                    logService.createLog(projectId, LogLevel.DEBUG, role, stageType,
                            String.format("토큰 사용: 입력 %d / 출력 %d", result.getInputTokens(), result.getOutputTokens()));
                }

                logService.createLog(projectId, LogLevel.INFO, role, stageType,
                        String.format("%s 완료! 다음 팀에게 넘길게요.", getAgentDisplayName(role)));

                auditService.log(AuditEventType.STAGE_COMPLETED, projectId,
                        String.format("%s 스테이지 완료 (%s)", stageType, role), null);

            } else if (result.getStatus() == AgentResult.Status.NEEDS_REVIEW) {
                log.info("[스테이지 실행] QA 검토 필요 - projectId={}, stage={}",
                        projectId, stageType);

                logService.createLog(projectId, LogLevel.WARN, role, stageType,
                        "QA 테스트에서 버그가 발견되었습니다. 디버깅이 필요합니다.");

            } else {
                log.warn("[스테이지 실행] 에이전트 실패 - projectId={}, stage={}, error={}",
                        projectId, stageType, result.getErrorMessage());

                stage.fail(result.getErrorMessage());
                logService.createErrorLog(projectId, role, stageType,
                        result.getErrorMessage(), "NT-ERR-A004", null);

                auditService.log(AuditEventType.STAGE_FAILED, projectId,
                        String.format("%s 스테이지 실패 (%s): %s", stageType, role, result.getErrorMessage()), null);

                sseEmitterService.sendEvent(projectId, SseEvent.error(
                        String.format("{\"code\":\"NT-ERR-A004\",\"agentRole\":\"%s\",\"message\":\"%s\"}",
                                role, result.getErrorMessage())));
            }

            return result;

        } catch (Exception e) {
            log.error("[스테이지 실행] 에이전트 예외 발생 - projectId={}, stage={}, agent={}, error={}",
                    projectId, stageType, role, e.getMessage(), e);

            stage.fail(e.getMessage());
            logService.createErrorLog(projectId, role, stageType,
                    e.getMessage(), "NT-ERR-A004", e.toString());

            auditService.log(AuditEventType.STAGE_FAILED, projectId,
                    String.format("%s 스테이지 예외 (%s): %s", stageType, role, e.getMessage()), null);

            return AgentResult.failed(e.getMessage());
        }
    }

    private String getAgentDisplayName(AgentRole role) {
        return switch (role) {
            case CTO -> "CTO";
            case PLANNING -> "기획팀";
            case DESIGN -> "디자인팀";
            case FRONTEND -> "프론트팀";
            case BACKEND -> "백엔드팀";
            case QA -> "QA팀";
            case CONTENT -> "콘텐츠팀";
            case SYSTEM -> "시스템";
        };
    }
}
