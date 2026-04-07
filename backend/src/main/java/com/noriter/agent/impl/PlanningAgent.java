package com.noriter.agent.impl;

import com.noriter.agent.core.*;
import com.noriter.agent.prompt.PromptRegistry;
import com.noriter.agent.prompt.PromptTemplate;
import com.noriter.domain.enums.AgentRole;
import com.noriter.infrastructure.claude.ClaudeApiClient;
import com.noriter.infrastructure.claude.ClaudeApiClient.ClaudeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Map;

@Log4j2
@Component
@RequiredArgsConstructor
public class PlanningAgent implements BaseAgent {

    private final ClaudeApiClient claudeApiClient;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentRole getRole() {
        return AgentRole.PLANNING;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[기획팀] 게임 기획서 작성 시작 - projectId={}", context.getProjectId());

        String systemPrompt = promptRegistry.getSystemPrompt("plan-main");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("plan-main"),
                Map.of(
                        "requirement", context.getRequirement(),
                        "genre", context.getGenre() != null ? context.getGenre() : "자유"
                )
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());

        log.info("[기획팀] 게임 기획서 작성 완료 - projectId={}", context.getProjectId());

        return AgentResult.success(
                Map.of("plan.json", response.content()),
                "게임 기획서 작성을 완료했습니다.",
                response.inputTokens(), response.outputTokens()
        );
    }
}
