package com.noriter.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.noriter.domain.AgentMessageEntity;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API-LOG-002: 에이전트 대화 메시지 응답
 * 참조: 05_API §4 API-LOG-002 응답
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentMessageResponse {

    private final String id;
    private final String fromAgent;
    private final String toAgent;
    private final String type;
    private final String content;
    private final String artifactRef;
    private final LocalDateTime timestamp;

    public AgentMessageResponse(AgentMessageEntity msg) {
        this.id = msg.getId();
        this.fromAgent = msg.getFromAgent().name();
        this.toAgent = msg.getToAgent().name();
        this.type = msg.getType().name();
        this.content = msg.getContent();
        this.artifactRef = msg.getArtifactRef();
        this.timestamp = msg.getCreatedAt();
    }
}
