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

/**
 * 콘텐츠 데이터 생성 에이전트 (STAGE 1.5 — PlanningAgent 직후)
 *
 * plan.json의 content 필드를 받아 게임에서 사용할 완전한 콘텐츠 데이터를 생성.
 * 예: 고양이 100종 목록, 맵 데이터, 아이템 목록 등.
 * 산출물: content.json → BackendAgent가 CAT_DATABASE 등 코드 상수로 직접 사용.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class ContentAgent implements BaseAgent {

    private final ClaudeApiClient claudeApiClient;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentRole getRole() {
        return AgentRole.CONTENT;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[콘텐츠팀] 콘텐츠 데이터 생성 시작 - projectId={}", context.getProjectId());

        String plan = context.getPreviousArtifacts().getOrDefault("plan.json", "");

        // plan.json에 content 필드가 없으면 건너뜀
        if (!plan.contains("\"content\"")) {
            log.info("[콘텐츠팀] plan.json에 content 없음, 건너뜀 - projectId={}", context.getProjectId());
            return AgentResult.success(Map.of(), "기획서에 별도 콘텐츠 데이터 정의가 없어서 건너뛸게요~", 0, 0);
        }

        String systemPrompt = promptRegistry.getSystemPrompt("content-main");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("content-main"),
                Map.of("plan", plan, "requirement", context.getRequirement())
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());
        String content = JsonParser.stripCodeBlock(response.content());

        log.info("[콘텐츠팀] 콘텐츠 데이터 생성 완료 - projectId={}, 크기={}자",
                context.getProjectId(), content.length());

        String message = extractChatMessage(content, "콘텐츠 데이터 완성! 백엔드팀, 상수로 바로 쓰시면 됩니다.");
        return AgentResult.success(
                Map.of("content.json", content),
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
