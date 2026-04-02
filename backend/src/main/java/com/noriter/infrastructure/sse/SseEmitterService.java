package com.noriter.infrastructure.sse;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * SSE Emitter 관리 서비스
 * 참조: 03_아키텍처 §5.2 SseEmitterService
 *
 * 프로젝트별로 여러 클라이언트가 구독할 수 있다.
 * 연결: 프로젝트 상세 페이지 진입 시 (status=IN_PROGRESS/REVISION)
 * 해제: 페이지 이탈 시 또는 complete/cancelled 이벤트 수신 시
 */
@Log4j2
@Service
public class SseEmitterService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;  // 30분 (파이프라인 최대 시간)

    // projectId → 구독 중인 Emitter 목록
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 새 SSE 연결 생성
     * 참조: API-SSE-001 GET /api/projects/{id}/stream
     */
    public SseEmitter createEmitter(String projectId) {
        log.info("[SSE] 새 연결 생성 - projectId={}", projectId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitters.computeIfAbsent(projectId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> {
            log.debug("[SSE] 연결 완료(정상 종료) - projectId={}", projectId);
            removeEmitter(projectId, emitter);
        });

        emitter.onTimeout(() -> {
            log.warn("[SSE] 연결 타임아웃 - projectId={}", projectId);
            removeEmitter(projectId, emitter);
        });

        emitter.onError(e -> {
            log.warn("[SSE] 연결 에러 - projectId={}, error={}", projectId, e.getMessage());
            removeEmitter(projectId, emitter);
        });

        int totalConnections = emitters.getOrDefault(projectId, List.of()).size();
        log.info("[SSE] 연결 등록 완료 - projectId={}, 현재 구독자 수={}", projectId, totalConnections);

        // 연결 확인용 초기 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"message\":\"SSE 연결 성공\",\"projectId\":\"" + projectId + "\"}"));
        } catch (IOException e) {
            log.error("[SSE] 초기 이벤트 전송 실패 - projectId={}", projectId);
        }

        return emitter;
    }

    /**
     * 프로젝트 구독자에게 이벤트 전송
     * 참조: 03_아키텍처 §5.2 SSE 이벤트 타입 6종
     */
    public void sendEvent(String projectId, SseEvent event) {
        List<SseEmitter> projectEmitters = emitters.get(projectId);
        if (projectEmitters == null || projectEmitters.isEmpty()) {
            log.debug("[SSE] 구독자 없음, 이벤트 무시 - projectId={}, eventType={}", projectId, event.getEventType());
            return;
        }

        log.debug("[SSE] 이벤트 전송 - projectId={}, eventType={}, 구독자 수={}",
                projectId, event.getEventType(), projectEmitters.size());

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : projectEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getEventType())
                        .data(event.getData()));
            } catch (IOException e) {
                log.debug("[SSE] 전송 실패 (연결 끊김) - projectId={}", projectId);
                deadEmitters.add(emitter);
            }
        }

        // 죽은 연결 정리
        if (!deadEmitters.isEmpty()) {
            projectEmitters.removeAll(deadEmitters);
            log.debug("[SSE] 끊긴 연결 정리 - projectId={}, 제거={}개, 남음={}개",
                    projectId, deadEmitters.size(), projectEmitters.size());
        }
    }

    /**
     * 프로젝트의 모든 SSE 연결 종료
     * complete 또는 cancelled 이벤트 후 호출
     */
    public void completeAll(String projectId) {
        log.info("[SSE] 프로젝트 전체 연결 종료 - projectId={}", projectId);

        List<SseEmitter> projectEmitters = emitters.remove(projectId);
        if (projectEmitters != null) {
            for (SseEmitter emitter : projectEmitters) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("[SSE] Emitter 종료 중 예외 (무시) - projectId={}", projectId);
                }
            }
            log.info("[SSE] 전체 연결 종료 완료 - projectId={}, 종료된 연결={}개",
                    projectId, projectEmitters.size());
        }
    }

    /**
     * 현재 활성 프로젝트 수 (헬스체크용)
     */
    public int getActiveProjectCount() {
        return emitters.size();
    }

    private void removeEmitter(String projectId, SseEmitter emitter) {
        List<SseEmitter> projectEmitters = emitters.get(projectId);
        if (projectEmitters != null) {
            projectEmitters.remove(emitter);
            if (projectEmitters.isEmpty()) {
                emitters.remove(projectId);
            }
        }
    }
}
