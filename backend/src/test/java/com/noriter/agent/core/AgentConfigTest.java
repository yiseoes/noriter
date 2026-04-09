package com.noriter.agent.core;

import com.noriter.domain.enums.AgentRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentConfigTest {

    @Test
    @DisplayName("기획팀은 temperature 0.7, maxTokens 8192이다")
    void planningAgent_hasCorrectConfig() {
        AgentConfig config = AgentConfig.forRole(AgentRole.PLANNING);
        assertThat(config.getTemperature()).isEqualTo(0.7);
        assertThat(config.getMaxTokens()).isEqualTo(8192);
    }

    @Test
    @DisplayName("CTO는 temperature 0.3이다 (보수적 판단)")
    void ctoAgent_hasCorrectConfig() {
        AgentConfig config = AgentConfig.forRole(AgentRole.CTO);
        assertThat(config.getTemperature()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("디자인팀은 temperature 0.8이다 (창의적 디자인)")
    void designAgent_hasCorrectConfig() {
        AgentConfig config = AgentConfig.forRole(AgentRole.DESIGN);
        assertThat(config.getTemperature()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("프론트팀은 maxTokens 32768이다 (고퀄 코드 생성)")
    void frontendAgent_hasCorrectConfig() {
        AgentConfig config = AgentConfig.forRole(AgentRole.FRONTEND);
        assertThat(config.getMaxTokens()).isEqualTo(32768);
        assertThat(config.getTemperature()).isEqualTo(0.4);
    }

    @Test
    @DisplayName("백엔드팀은 maxTokens 32768이다 (고퀄 코드 생성)")
    void backendAgent_hasCorrectConfig() {
        AgentConfig config = AgentConfig.forRole(AgentRole.BACKEND);
        assertThat(config.getMaxTokens()).isEqualTo(32768);
        assertThat(config.getTemperature()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("QA팀은 temperature 0.2, maxTokens 8192이다")
    void qaAgent_hasCorrectConfig() {
        AgentConfig config = AgentConfig.forRole(AgentRole.QA);
        assertThat(config.getTemperature()).isEqualTo(0.2);
        assertThat(config.getMaxTokens()).isEqualTo(8192);
    }

    @Test
    @DisplayName("모든 에이전트의 모델은 claude-sonnet-4-6이다")
    void allAgents_useSameModel() {
        for (AgentRole role : AgentRole.values()) {
            if (role == AgentRole.SYSTEM) continue;
            assertThat(AgentConfig.forRole(role).getModel()).isEqualTo("claude-sonnet-4-6");
        }
    }
}
