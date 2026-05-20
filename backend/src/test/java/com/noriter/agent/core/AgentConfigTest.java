package com.noriter.agent.core;

import com.noriter.domain.enums.AgentRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    @DisplayName("프론트팀은 maxTokens 16384이다 (코드 생성, 분할 전략 대비)")
    void frontendAgent_hasCorrectConfig() {
        AgentConfig config = AgentConfig.forRole(AgentRole.FRONTEND);
        assertThat(config.getMaxTokens()).isEqualTo(16384);
        assertThat(config.getTemperature()).isEqualTo(0.4);
    }

    @Test
    @DisplayName("백엔드팀은 maxTokens 16384이다 (코드 생성, 분할 전략 대비)")
    void backendAgent_hasCorrectConfig() {
        AgentConfig config = AgentConfig.forRole(AgentRole.BACKEND);
        assertThat(config.getMaxTokens()).isEqualTo(16384);
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
    @DisplayName("코드 생성 에이전트(Backend/Frontend/QA)는 claude-sonnet-4-6을 사용한다")
    void codeGenerationAgents_useSonnet() {
        for (AgentRole role : List.of(AgentRole.BACKEND, AgentRole.FRONTEND, AgentRole.QA)) {
            assertThat(AgentConfig.forRole(role).getModel())
                    .as(role + " 모델")
                    .isEqualTo("claude-sonnet-4-6");
        }
    }

    @Test
    @DisplayName("기획/설계 에이전트(Planning/Content/CTO/Design)는 claude-haiku를 사용한다")
    void planningAgents_useHaiku() {
        for (AgentRole role : List.of(AgentRole.PLANNING, AgentRole.CONTENT, AgentRole.CTO, AgentRole.DESIGN)) {
            assertThat(AgentConfig.forRole(role).getModel())
                    .as(role + " 모델")
                    .startsWith("claude-haiku");
        }
    }
}
