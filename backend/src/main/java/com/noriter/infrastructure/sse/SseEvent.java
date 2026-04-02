package com.noriter.infrastructure.sse;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SSE 이벤트 모델
 * 참조: 03_아키텍처 §5.2, API-SSE-001 이벤트 타입 6종
 */
@Getter
@AllArgsConstructor
public class SseEvent {

    private final String eventType;  // log, stage-update, agent-msg, error, complete, cancelled
    private final String data;       // JSON 문자열

    public static SseEvent log(String data) {
        return new SseEvent("log", data);
    }

    public static SseEvent stageUpdate(String data) {
        return new SseEvent("stage-update", data);
    }

    public static SseEvent agentMessage(String data) {
        return new SseEvent("agent-msg", data);
    }

    public static SseEvent error(String data) {
        return new SseEvent("error", data);
    }

    public static SseEvent complete(String data) {
        return new SseEvent("complete", data);
    }

    public static SseEvent cancelled(String data) {
        return new SseEvent("cancelled", data);
    }
}
