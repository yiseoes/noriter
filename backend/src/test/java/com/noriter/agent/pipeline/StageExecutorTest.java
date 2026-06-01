package com.noriter.agent.pipeline;

import com.noriter.agent.core.AgentContext;
import com.noriter.agent.core.AgentResult;
import com.noriter.agent.core.BaseAgent;
import com.noriter.agent.message.MessageBus;
import com.noriter.domain.Project;
import com.noriter.domain.Stage;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.StageStatus;
import com.noriter.domain.enums.StageType;
import com.noriter.infrastructure.sse.SseEmitterService;
import com.noriter.repository.StageRepository;
import com.noriter.service.AuditService;
import com.noriter.service.LogService;
import com.noriter.service.TokenUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StageExecutor 단위 테스트
 *
 * 구조적 리팩토링(StageExecutor가 Stage 라이프사이클 단일 소유) 이후
 * 핵심 보장 사항을 검증한다:
 *
 * 1. 에이전트 실행 전 stage.start() → stageRepository.save() 호출
 * 2. SUCCESS 시 stage IN_PROGRESS 유지 (complete는 PipelineOrchestrator 담당)
 * 3. FAILED 시 stage.fail() → stageRepository.save() 호출 (BUG-6A/7A 수정 핵심)
 * 4. 예외 발생 시 stage.fail() → stageRepository.save() 호출
 * 5. BUG-7C: messageBus 예외가 SUCCESS/NEEDS_REVIEW 결과를 FAILED로 바꾸면 안 됨
 * 6. 토큰 사용량 기록 조건 (0 초과 시만 기록)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StageExecutor — Stage 라이프사이클 + 결과 처리")
class StageExecutorTest {

    @InjectMocks StageExecutor stageExecutor;

    @Mock LogService          logService;
    @Mock AuditService        auditService;
    @Mock TokenUsageService   tokenUsageService;
    @Mock SseEmitterService   sseEmitterService;
    @Mock MessageBus          messageBus;
    @Mock StageRepository     stageRepository;
    @Mock BaseAgent           agent;

    private Stage        stage;
    private AgentContext context;

    @BeforeEach
    void setUp() {
        Project project = Project.create("test", "테스트 요구사항 10자 이상", null, 3, false, null, null);
        stage = Stage.create(project, StageType.PLANNING, "PLANNING", 1);

        context = AgentContext.builder()
                .projectId("prj_test")
                .stageType(StageType.PLANNING)
                .requirement("테스트 요구사항")
                .previousArtifacts(Map.of())
                .debugAttempt(0)
                .build();

        lenient().when(agent.getRole()).thenReturn(AgentRole.PLANNING);
        lenient().when(stageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // =========================================================================
    // 1. 실행 전: stage.start() → stageRepository.save() 호출
    // =========================================================================

    @Test
    @DisplayName("에이전트 실행 전 stage.start() 와 stageRepository.save() 가 호출된다")
    void executeAgent_callsStartAndSaveBeforeExecution() {
        when(agent.execute(any())).thenAnswer(invocation -> {
            // 에이전트 execute() 호출 시점에 stage가 이미 IN_PROGRESS여야 함
            assertThat(stage.getStatus())
                    .as("에이전트 실행 시점에 stage가 IN_PROGRESS 상태여야 함")
                    .isEqualTo(StageStatus.IN_PROGRESS);
            return AgentResult.success(Map.of("plan.json", "{}"), "", 0, 0);
        });

        stageExecutor.executeAgent(agent, context, stage);

        verify(stageRepository, atLeastOnce()).save(stage);
    }

    // =========================================================================
    // 2. SUCCESS: stage IN_PROGRESS 유지, save() 1회 (start 시점)
    // =========================================================================

    @Test
    @DisplayName("SUCCESS: stage가 IN_PROGRESS 상태로 유지되고 stageRepository.save() 가 1회 호출된다")
    void executeAgent_success_stageRemainsInProgress() {
        when(agent.execute(any())).thenReturn(
                AgentResult.success(Map.of("plan.json", "{}"), "완료", 100, 50));

        AgentResult result = stageExecutor.executeAgent(agent, context, stage);

        assertThat(result.getStatus()).isEqualTo(AgentResult.Status.SUCCESS);
        assertThat(result.getArtifacts()).containsKey("plan.json");

        // SUCCESS 시 StageExecutor는 complete() 미호출 — PipelineOrchestrator 담당
        assertThat(stage.getStatus()).isEqualTo(StageStatus.IN_PROGRESS);

        // save()는 start() 시점 1회만 호출됨
        verify(stageRepository, times(1)).save(stage);
    }

    // =========================================================================
    // 3. FAILED: stage.fail() → stageRepository.save() 호출 (BUG-6A/7A 수정 핵심)
    // =========================================================================

    @Test
    @DisplayName("FAILED: stage.fail() 후 stageRepository.save() 가 호출되고 에러 메시지가 저장된다 (BUG-6A/7A 수정 검증)")
    void executeAgent_agentReturnsFailed_stageFailedAndPersisted() {
        when(agent.execute(any())).thenReturn(AgentResult.failed("Claude API 타임아웃"));

        AgentResult result = stageExecutor.executeAgent(agent, context, stage);

        // 결과 검증
        assertThat(result.getStatus()).isEqualTo(AgentResult.Status.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Claude API 타임아웃");

        // Stage 상태 검증
        assertThat(stage.getStatus())
                .as("agent FAILED 시 stage가 FAILED 상태여야 함")
                .isEqualTo(StageStatus.FAILED);
        assertThat(stage.getErrorMessage())
                .as("에러 메시지가 stage에 저장되어야 함")
                .isEqualTo("Claude API 타임아웃");

        // save() 2회: start() + fail()
        verify(stageRepository, times(2)).save(stage);
    }

    // =========================================================================
    // 4. 예외: stage.fail() → stageRepository.save() 호출
    // =========================================================================

    @Test
    @DisplayName("예외 발생: stage.fail() 후 stageRepository.save() 가 호출되고 AgentResult.failed() 가 반환된다")
    void executeAgent_agentThrowsException_stageFailedAndPersisted() {
        when(agent.execute(any())).thenThrow(new RuntimeException("네트워크 오류"));

        AgentResult result = stageExecutor.executeAgent(agent, context, stage);

        // 결과 검증
        assertThat(result.getStatus()).isEqualTo(AgentResult.Status.FAILED);
        assertThat(result.getErrorMessage()).contains("네트워크 오류");

        // Stage 상태 검증
        assertThat(stage.getStatus())
                .as("예외 발생 시 stage가 FAILED 상태여야 함")
                .isEqualTo(StageStatus.FAILED);

        // save() 2회: start() + 예외 처리
        verify(stageRepository, times(2)).save(stage);
    }

    // =========================================================================
    // 5. BUG-7C: SUCCESS + messageBus 예외 → SUCCESS 결과 + artifacts 보존
    // =========================================================================

    @Test
    @DisplayName("BUG-7C: SUCCESS 후 messageBus.send() 예외 발생해도 SUCCESS 결과와 artifacts 가 보존되어야 한다")
    void executeAgent_success_messageBusThrows_stillReturnsSuccess() {
        when(agent.execute(any())).thenReturn(
                AgentResult.success(Map.of("plan.json", "{\"title\":\"뱀게임\"}"), "완료 메시지", 500, 200));
        when(messageBus.send(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("SSE 연결 끊김"));

        AgentResult result = stageExecutor.executeAgent(agent, context, stage);

        assertThat(result.getStatus())
                .as("messageBus 예외가 SUCCESS 결과를 FAILED로 바꾸면 안 됨 (BUG-7C)")
                .isEqualTo(AgentResult.Status.SUCCESS);

        assertThat(result.getArtifacts())
                .as("messageBus 예외에도 artifacts 가 유실되면 안 됨")
                .containsKey("plan.json");

        assertThat(stage.getStatus())
                .as("messageBus 예외가 stage를 FAILED로 바꾸면 안 됨")
                .isNotEqualTo(StageStatus.FAILED);

        // save()는 start() 1회만 호출됨 (fail() 미호출)
        verify(stageRepository, times(1)).save(stage);
    }

    // =========================================================================
    // 6. BUG-7C: NEEDS_REVIEW + messageBus 예외 → NEEDS_REVIEW 결과 + artifacts 보존
    // =========================================================================

    @Test
    @DisplayName("BUG-7C: NEEDS_REVIEW 후 messageBus.send() 예외 발생해도 NEEDS_REVIEW 결과와 test-report.json 이 보존되어야 한다")
    void executeAgent_needsReview_messageBusThrows_stillReturnsNeedsReview() {
        String testReport = "{\"result\":\"FAIL\",\"bugs\":[{\"severity\":\"HIGH\"}]}";
        when(agent.execute(any())).thenReturn(
                AgentResult.needsReview(Map.of("test-report.json", testReport), "버그 발견", 600, 300));
        when(messageBus.send(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("SSE 오류"));

        AgentResult result = stageExecutor.executeAgent(agent, context, stage);

        assertThat(result.getStatus())
                .as("messageBus 예외가 NEEDS_REVIEW 결과를 FAILED로 바꾸면 안 됨 (BUG-7C)")
                .isEqualTo(AgentResult.Status.NEEDS_REVIEW);

        assertThat(result.getArtifacts())
                .as("test-report.json 이 유실되면 안 됨 — CTO debug context에 필요")
                .containsKey("test-report.json");

        assertThat(stage.getStatus())
                .as("messageBus 예외가 stage를 FAILED로 바꾸면 안 됨")
                .isNotEqualTo(StageStatus.FAILED);
    }

    // =========================================================================
    // 7. 토큰 사용량 기록
    // =========================================================================

    @Test
    @DisplayName("토큰이 0보다 크면 tokenUsageService.record() 가 호출된다")
    void executeAgent_withTokens_recordsUsage() {
        when(agent.execute(any())).thenReturn(
                AgentResult.success(Map.of("plan.json", "{}"), "", 1000, 500));

        stageExecutor.executeAgent(agent, context, stage);

        verify(tokenUsageService).record(
                eq("prj_test"), eq(AgentRole.PLANNING), eq(StageType.PLANNING),
                eq(1000), eq(500));
    }

    @Test
    @DisplayName("토큰이 0이면 tokenUsageService.record() 가 호출되지 않는다")
    void executeAgent_withZeroTokens_doesNotRecord() {
        when(agent.execute(any())).thenReturn(
                AgentResult.success(Map.of("plan.json", "{}"), "", 0, 0));

        stageExecutor.executeAgent(agent, context, stage);

        verify(tokenUsageService, never()).record(any(), any(), any(), anyInt(), anyInt());
    }

    // =========================================================================
    // 8. FAILED 시 stageRepository.save() 미호출이면 DB 상태가 갱신 안 됨을 역으로 검증
    //    (리팩토링 전 버그 재현 — 이 테스트가 실패하면 리팩토링이 되돌아간 것)
    // =========================================================================

    @Test
    @DisplayName("FAILED: stageRepository.save() 가 2회 호출되는지 정확히 검증 (1회면 start만 저장된 버그 상태)")
    void executeAgent_agentReturnsFailed_saveCalledExactlyTwice() {
        when(agent.execute(any())).thenReturn(AgentResult.failed("에러"));

        stageExecutor.executeAgent(agent, context, stage);

        // 정확히 2회: 1회(start) + 1회(fail)
        // 만약 1회라면 리팩토링 이전 BUG-6A 상태로 돌아간 것
        verify(stageRepository, times(2)).save(stage);
    }

    @Test
    @DisplayName("예외: stageRepository.save() 가 2회 호출되는지 정확히 검증")
    void executeAgent_exception_saveCalledExactlyTwice() {
        when(agent.execute(any())).thenThrow(new IllegalStateException("예상치 못한 오류"));

        stageExecutor.executeAgent(agent, context, stage);

        verify(stageRepository, times(2)).save(stage);
    }

    // =========================================================================
    // 9. NEEDS_REVIEW: stage IN_PROGRESS 유지 (QA 결과 — PipelineOrchestrator가 처리)
    // =========================================================================

    @Test
    @DisplayName("NEEDS_REVIEW: stage가 IN_PROGRESS 상태로 유지되고 결과가 그대로 반환된다")
    void executeAgent_needsReview_stageRemainsInProgress() {
        when(agent.execute(any())).thenReturn(
                AgentResult.needsReview(Map.of("test-report.json", "{}"), "버그 발견", 300, 150));

        AgentResult result = stageExecutor.executeAgent(agent, context, stage);

        assertThat(result.getStatus()).isEqualTo(AgentResult.Status.NEEDS_REVIEW);
        assertThat(stage.getStatus()).isEqualTo(StageStatus.IN_PROGRESS);
        verify(stageRepository, times(1)).save(stage);
    }
}
