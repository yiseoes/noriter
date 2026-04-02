package com.noriter.infrastructure.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;

class SseEmitterServiceTest {

    private SseEmitterService sseEmitterService;

    @BeforeEach
    void setUp() {
        sseEmitterService = new SseEmitterService();
    }

    @Test
    @DisplayName("SSE Emitter를 생성할 수 있다")
    void createEmitter_returnsEmitter() {
        SseEmitter emitter = sseEmitterService.createEmitter("prj_test");

        assertThat(emitter).isNotNull();
        assertThat(sseEmitterService.getActiveProjectCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 프로젝트에 여러 Emitter를 등록할 수 있다")
    void createMultipleEmitters_sameProject() {
        sseEmitterService.createEmitter("prj_test");
        sseEmitterService.createEmitter("prj_test");

        // 프로젝트 수는 1개
        assertThat(sseEmitterService.getActiveProjectCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 프로젝트에 Emitter를 등록할 수 있다")
    void createEmitters_differentProjects() {
        sseEmitterService.createEmitter("prj_aaa");
        sseEmitterService.createEmitter("prj_bbb");

        assertThat(sseEmitterService.getActiveProjectCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("구독자가 없어도 이벤트 전송이 에러 없이 처리된다")
    void sendEvent_noSubscribers_noError() {
        // 구독자 없는 프로젝트에 이벤트 전송 — 예외 없이 무시
        sseEmitterService.sendEvent("prj_none", SseEvent.log("{\"message\":\"테스트\"}"));

        assertThat(sseEmitterService.getActiveProjectCount()).isZero();
    }

    @Test
    @DisplayName("completeAll로 프로젝트의 모든 연결을 종료할 수 있다")
    void completeAll_removesAllEmitters() {
        sseEmitterService.createEmitter("prj_test");
        sseEmitterService.createEmitter("prj_test");
        assertThat(sseEmitterService.getActiveProjectCount()).isEqualTo(1);

        sseEmitterService.completeAll("prj_test");

        assertThat(sseEmitterService.getActiveProjectCount()).isZero();
    }

    @Test
    @DisplayName("SseEvent 정적 팩토리 메서드가 올바른 타입을 생성한다")
    void sseEvent_factoryMethods() {
        assertThat(SseEvent.log("data").getEventType()).isEqualTo("log");
        assertThat(SseEvent.stageUpdate("data").getEventType()).isEqualTo("stage-update");
        assertThat(SseEvent.agentMessage("data").getEventType()).isEqualTo("agent-msg");
        assertThat(SseEvent.error("data").getEventType()).isEqualTo("error");
        assertThat(SseEvent.complete("data").getEventType()).isEqualTo("complete");
        assertThat(SseEvent.cancelled("data").getEventType()).isEqualTo("cancelled");
    }
}
