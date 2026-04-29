package com.noriter.agent.impl;

import com.noriter.agent.core.*;
import com.noriter.agent.prompt.PromptRegistry;
import com.noriter.agent.prompt.PromptTemplate;
import com.noriter.domain.enums.AgentRole;
import com.noriter.infrastructure.claude.ClaudeApiClient;
import com.noriter.infrastructure.claude.ClaudeApiClient.ClaudeResponse;
import com.noriter.util.JsonParser;
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

        String message = extractChatMessage(response.content(), "기획서 완성! CTO님, 아키텍처 설계 부탁드려요.");
        return AgentResult.success(
                Map.of("plan.json", response.content()),
                message,
                response.inputTokens(), response.outputTokens()
        );
    }

    private String extractChatMessage(String json, String fallback) {
        com.fasterxml.jackson.databind.JsonNode node = JsonParser.parse(json);
        if (node != null && node.has("chatMessage") && !node.get("chatMessage").asText().isBlank()) {
            return node.get("chatMessage").asText();
        }
        return fallback;
    }
}
