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

    // Haiku: 기획/설계/디자인/콘텐츠 (JSON 생성 위주, 비용 절감)
    // Sonnet: 코드 생성(Frontend/Backend) + QA (품질 중요)
    private static final Map<AgentRole, AgentConfig> CONFIGS = Map.of(
            AgentRole.PLANNING, new AgentConfig("claude-haiku-4-5-20251001", 8192, 0.7),
            AgentRole.CONTENT,  new AgentConfig("claude-haiku-4-5-20251001", 8192, 0.5),
            AgentRole.CTO,      new AgentConfig("claude-haiku-4-5-20251001", 8192, 0.3),
            AgentRole.DESIGN,   new AgentConfig("claude-haiku-4-5-20251001", 8192, 0.8),
            AgentRole.FRONTEND, new AgentConfig("claude-sonnet-4-6", 16384, 0.4),
            AgentRole.BACKEND,  new AgentConfig("claude-sonnet-4-6", 16384, 0.3),
            AgentRole.QA,       new AgentConfig("claude-sonnet-4-6", 8192, 0.2)
    );

    public static AgentConfig forRole(AgentRole role) {
        return CONFIGS.getOrDefault(role,
                new AgentConfig("claude-haiku-4-5-20251001", 4096, 0.5));
    }
}
