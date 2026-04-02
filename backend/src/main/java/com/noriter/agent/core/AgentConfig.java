package com.noriter.agent.core;

import com.noriter.domain.enums.AgentRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 에이전트별 Claude API 설정
 * 참조: 04_에이전트 §4 에이전트 설정 총괄, 08_프롬프트 §3
 */
@Getter
@AllArgsConstructor
public class AgentConfig {

    private final String model;
    private final int maxTokens;
    private final double temperature;

    private static final Map<AgentRole, AgentConfig> CONFIGS = Map.of(
            AgentRole.PLANNING, new AgentConfig("claude-sonnet-4-6", 4096, 0.7),
            AgentRole.CTO,      new AgentConfig("claude-sonnet-4-6", 4096, 0.3),
            AgentRole.DESIGN,   new AgentConfig("claude-sonnet-4-6", 4096, 0.8),
            AgentRole.FRONTEND, new AgentConfig("claude-sonnet-4-6", 8192, 0.4),
            AgentRole.BACKEND,  new AgentConfig("claude-sonnet-4-6", 8192, 0.3),
            AgentRole.QA,       new AgentConfig("claude-sonnet-4-6", 4096, 0.2)
    );

    public static AgentConfig forRole(AgentRole role) {
        return CONFIGS.getOrDefault(role,
                new AgentConfig("claude-sonnet-4-6", 4096, 0.5));
    }
}
