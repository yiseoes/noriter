package com.noriter.domain;

import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.StageType;
import com.noriter.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_usage")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenUsage {

    @Id
    @Column(length = 20)
    private String id;

    @Column(name = "project_id", nullable = false, length = 20)
    private String projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_role", nullable = false, length = 20)
    private AgentRole agentRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StageType stage;

    @Column(name = "input_tokens", nullable = false)
    private Integer inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private Integer outputTokens;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static TokenUsage create(String projectId, AgentRole agentRole, StageType stage,
                                     int inputTokens, int outputTokens) {
        TokenUsage usage = new TokenUsage();
        usage.id = IdGenerator.generateTokenUsageId();
        usage.projectId = projectId;
        usage.agentRole = agentRole;
        usage.stage = stage;
        usage.inputTokens = inputTokens;
        usage.outputTokens = outputTokens;
        usage.createdAt = LocalDateTime.now();
        return usage;
    }

    public int getTotalTokens() {
        return inputTokens + outputTokens;
    }
}
