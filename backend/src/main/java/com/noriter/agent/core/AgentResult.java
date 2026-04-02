package com.noriter.agent.core;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 에이전트 실행 결과
 * 참조: 03_아키텍처 §3.3 AgentResult
 */
@Getter
@Builder
public class AgentResult {

    public enum Status {
        SUCCESS,
        FAILED,
        NEEDS_REVIEW   // QA 실패 시 — 디버깅 필요
    }

    private final Status status;
    private final Map<String, String> artifacts;    // key=파일명, value=내용
    private final String message;                   // 다음 에이전트에게 보낼 메시지
    private final String errorMessage;              // 실패 시 에러 메시지
    private final int inputTokens;
    private final int outputTokens;

    public static AgentResult success(Map<String, String> artifacts, String message,
                                       int inputTokens, int outputTokens) {
        return AgentResult.builder()
                .status(Status.SUCCESS)
                .artifacts(artifacts)
                .message(message)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .build();
    }

    public static AgentResult failed(String errorMessage) {
        return AgentResult.builder()
                .status(Status.FAILED)
                .errorMessage(errorMessage)
                .build();
    }

    public static AgentResult needsReview(Map<String, String> artifacts, String message,
                                           int inputTokens, int outputTokens) {
        return AgentResult.builder()
                .status(Status.NEEDS_REVIEW)
                .artifacts(artifacts)
                .message(message)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .build();
    }
}
