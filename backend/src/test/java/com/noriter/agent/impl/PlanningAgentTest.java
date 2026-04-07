package com.noriter.agent.impl;

import com.noriter.agent.core.AgentContext;
import com.noriter.agent.core.AgentResult;
import com.noriter.agent.prompt.PromptRegistry;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.StageType;
import com.noriter.infrastructure.claude.ClaudeApiClient;
import com.noriter.infrastructure.claude.ClaudeApiClient.ClaudeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanningAgentTest {

    @InjectMocks
    private PlanningAgent agent;

    @Mock
    private ClaudeApiClient claudeApiClient;

    @Mock
    private PromptRegistry promptRegistry;

    @Test
    @DisplayName("기획팀 에이전트의 역할은 PLANNING이다")
    void getRole_returnsPLANNING() {
        assertThat(agent.getRole()).isEqualTo(AgentRole.PLANNING);
    }

    @Test
    @DisplayName("기획서(plan.json)를 성공적으로 생성한다")
    void execute_returnsPlanJson() {
        when(promptRegistry.getSystemPrompt("plan-main")).thenReturn("system prompt");
        when(promptRegistry.getUserPrompt("plan-main")).thenReturn("{{requirement}} {{genre}}");
        when(claudeApiClient.sendPrompt(anyString(), anyString(), eq(AgentRole.PLANNING)))
                .thenReturn(new ClaudeResponse("{\"gameName\":\"테스트 게임\"}", 500, 800));

        AgentContext context = AgentContext.builder()
                .projectId("prj_test")
                .stageType(StageType.PLANNING)
                .requirement("뱀파이어 서바이벌 류 미니게임을 만들어줘")
                .build();

        AgentResult result = agent.execute(context);

        assertThat(result.getStatus()).isEqualTo(AgentResult.Status.SUCCESS);
        assertThat(result.getArtifacts()).containsKey("plan.json");
        assertThat(result.getArtifacts().get("plan.json")).contains("gameName");
        assertThat(result.getInputTokens()).isEqualTo(500);
        assertThat(result.getOutputTokens()).isEqualTo(800);
    }
}
