package com.noriter.agent.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentResultTest {

    @Test
    @DisplayName("success 팩토리 메서드가 올바른 결과를 생성한다")
    void success_createsCorrectResult() {
        AgentResult result = AgentResult.success(
                Map.of("plan.json", "{}"), "완료", 100, 200);

        assertThat(result.getStatus()).isEqualTo(AgentResult.Status.SUCCESS);
        assertThat(result.getArtifacts()).containsKey("plan.json");
        assertThat(result.getMessage()).isEqualTo("완료");
        assertThat(result.getInputTokens()).isEqualTo(100);
        assertThat(result.getOutputTokens()).isEqualTo(200);
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("failed 팩토리 메서드가 에러 메시지를 포함한다")
    void failed_containsErrorMessage() {
        AgentResult result = AgentResult.failed("Claude API 호출 실패");

        assertThat(result.getStatus()).isEqualTo(AgentResult.Status.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Claude API 호출 실패");
        assertThat(result.getArtifacts()).isNull();
    }

    @Test
    @DisplayName("needsReview 팩토리 메서드가 QA 실패 상태를 생성한다")
    void needsReview_createsReviewResult() {
        AgentResult result = AgentResult.needsReview(
                Map.of("test-report.json", "{}"), "버그 발견", 300, 400);

        assertThat(result.getStatus()).isEqualTo(AgentResult.Status.NEEDS_REVIEW);
        assertThat(result.getArtifacts()).containsKey("test-report.json");
        assertThat(result.getMessage()).contains("버그");
    }
}
