package com.noriter.agent.message;

import com.noriter.domain.AgentMessageEntity;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.MessageType;
import com.noriter.infrastructure.sse.SseEmitterService;
import com.noriter.infrastructure.sse.SseEvent;
import com.noriter.repository.AgentMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 에이전트 간 메시지 버스
 * 참조: 03_아키텍처 §3.5 MessageBus
 *
 * 메시지 전송 시:
 * 1. DB 저장 (감사 로그 겸용)
 * 2. SSE로 실시간 클라이언트 전달
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class MessageBus {

    private final AgentMessageRepository messageRepository;
    private final SseEmitterService sseEmitterService;

    /**
     * 에이전트 간 메시지 전송
     * 참조: 03_아키텍처 §3.5 send()
     */
    @Transactional
    public AgentMessageEntity send(String projectId, AgentRole from, AgentRole to,
                                    MessageType type, String content, String artifactRef) {
        log.info("[메시지 버스] 메시지 전송 - projectId={}, {} → {}, type={}, 내용 길이={}자",
                projectId, from, to, type, content.length());

        AgentMessageEntity message = AgentMessageEntity.create(
                projectId, from, to, type, content, artifactRef);
        messageRepository.save(message);

        log.info("[메시지 버스] DB 저장 완료 - id={}", message.getId());

        // SSE 실시간 전달 (07_화면 §10: agent-msg 이벤트)
        String sseData = String.format(
                "{\"fromAgent\":\"%s\",\"toAgent\":\"%s\",\"type\":\"%s\",\"content\":\"%s\"}",
                from, to, type, content.replace("\"", "\\\""));
        sseEmitterService.sendEvent(projectId, SseEvent.agentMessage(sseData));

        log.debug("[메시지 버스] SSE 전달 완료 - projectId={}", projectId);
        return message;
    }

    /**
     * 프로젝트의 전체 메시지 조회
     * 참조: 03_아키텍처 §3.5 getMessages()
     */
    public List<AgentMessageEntity> getMessages(String projectId) {
        log.debug("[메시지 버스] 메시지 조회 - projectId={}", projectId);
        return messageRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
    }

    /**
     * 특정 에이전트 관련 메시지 조회
     */
    public List<AgentMessageEntity> getMessages(String projectId, AgentRole agentRole) {
        log.debug("[메시지 버스] 에이전트별 메시지 조회 - projectId={}, agent={}", projectId, agentRole);
        return messageRepository.findByProjectIdAndAgentRole(projectId, agentRole);
    }
}
