package com.noriter.domain;

import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.MessageType;
import com.noriter.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentMessageEntity {

    @Id
    @Column(length = 20)
    private String id;

    @Column(name = "project_id", nullable = false, length = 20)
    private String projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_agent", nullable = false, length = 20)
    private AgentRole fromAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_agent", nullable = false, length = 20)
    private AgentRole toAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "artifact_ref", length = 20)
    private String artifactRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AgentMessageEntity create(String projectId, AgentRole from, AgentRole to,
                                             MessageType type, String content, String artifactRef) {
        AgentMessageEntity msg = new AgentMessageEntity();
        msg.id = IdGenerator.generateMessageId();
        msg.projectId = projectId;
        msg.fromAgent = from;
        msg.toAgent = to;
        msg.type = type;
        msg.content = content;
        msg.artifactRef = artifactRef;
        msg.createdAt = LocalDateTime.now();
        return msg;
    }
}
