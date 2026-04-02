package com.noriter.controller;

import com.noriter.infrastructure.sse.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 실시간 스트리밍 API
 * 참조: 05_API §3 API-SSE-001, 03_아키텍처 §5.2
 */
@Log4j2
@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    /**
     * API-SSE-001: 실시간 이벤트 스트림
     * GET /api/projects/{id}/stream
     * Content-Type: text/event-stream
     *
     * SSE 이벤트 타입 6종:
     * - log: 실시간 로그 (NT-MON-001)
     * - stage-update: 스테이지 상태 변경 (NT-MON-002)
     * - agent-msg: 에이전트 간 메시지 (NT-MON-003)
     * - error: 에러 발생 (NT-MON-004)
     * - complete: 파이프라인 완료
     * - cancelled: 파이프라인 취소
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable String id) {
        log.info("[API-SSE-001] SSE 스트림 연결 요청 - projectId={}", id);

        SseEmitter emitter = sseEmitterService.createEmitter(id);

        log.info("[API-SSE-001] SSE 스트림 연결 완료 - projectId={}", id);
        return emitter;
    }
}
