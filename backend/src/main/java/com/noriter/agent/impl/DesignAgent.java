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
public class DesignAgent implements BaseAgent {

    private final ClaudeApiClient claudeApiClient;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentRole getRole() {
        return AgentRole.DESIGN;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[디자인팀] UI/UX 디자인 스펙 작성 시작 - projectId={}", context.getProjectId());

        String plan = context.getPreviousArtifacts().getOrDefault("plan.json", "");
        String architecture = context.getPreviousArtifacts().getOrDefault("architecture.json", "");

        String systemPrompt = promptRegistry.getSystemPrompt("design-main");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("design-main"),
                Map.of("plan", plan, "architecture", architecture)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());

        log.info("[디자인팀] 디자인 스펙 작성 완료 - projectId={}", context.getProjectId());

        return AgentResult.success(
                Map.of("design.json", response.content()),
                "디자인 스펙 작성 완료.",
                response.inputTokens(), response.outputTokens()
        );
    }
}
