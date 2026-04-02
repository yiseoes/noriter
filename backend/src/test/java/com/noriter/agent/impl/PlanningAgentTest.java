package com.noriter.agent.impl;

import com.noriter.agent.core.AgentContext;
import com.noriter.agent.core.AgentResult;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.StageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlanningAgentTest {

    private final PlanningAgent agent = new PlanningAgent();

    @Test
    @DisplayName("기획팀 에이전트의 역할은 PLANNING이다")
    void getRole_returnsPLANNING() {
        assertThat(agent.getRole()).isEqualTo(AgentRole.PLANNING);
    }

    @Test
    @DisplayName("기획서(plan.json)를 성공적으로 생성한다")
    void execute_returnsPlanJson() {
        AgentContext context = AgentContext.builder()
                .projectId("prj_test")
                .stageType(StageType.PLANNING)
                .requirement("뱀파이어 서바이벌 류 미니게임을 만들어줘")
                .build();

        AgentResult result = agent.execute(context);

        assertThat(result.getStatus()).isEqualTo(AgentResult.Status.SUCCESS);
        assertThat(result.getArtifacts()).containsKey("plan.json");
        assertThat(result.getArtifacts().get("plan.json")).contains("gameName");
        assertThat(result.getMessage()).contains("기획서");
        assertThat(result.getInputTokens()).isGreaterThan(0);
        assertThat(result.getOutputTokens()).isGreaterThan(0);
    }
}
