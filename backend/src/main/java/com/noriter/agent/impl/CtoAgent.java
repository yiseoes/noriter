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
public class CtoAgent implements BaseAgent {

    private final ClaudeApiClient claudeApiClient;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentRole getRole() {
        return AgentRole.CTO;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[CTO] 기술 아키텍처 설계 시작 - projectId={}", context.getProjectId());

        String plan = context.getPreviousArtifacts().getOrDefault("plan.json", "");

        String systemPrompt = promptRegistry.getSystemPrompt("cto-main");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("cto-main"),
                Map.of("plan", plan)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());

        log.info("[CTO] 기술 아키텍처 설계 완료 - projectId={}", context.getProjectId());

        String message = extractChatMessage(response.content(), "아키텍처 설계 완료! 프론트/백엔드팀, 인터페이스 계약 확인하고 개발 시작해주세요.");
        return AgentResult.success(
                Map.of("architecture.json", response.content()),
                message,
                response.inputTokens(), response.outputTokens()
        );
    }

    /** game.js 프롬프트 포함 최대 글자 수 — 초과 시 앞뒤 발췌로 대체 */
    private static final int MAX_GAME_JS_CHARS = 30_000;

    public AgentResult executeFeedback(AgentContext context) {
        log.info("[CTO] 피드백 분석 시작 - projectId={}", context.getProjectId());

        String feedback = context.getFeedback() != null ? context.getFeedback() : "";
        Map<String, String> arts = context.getPreviousArtifacts();
        String plan = arts.getOrDefault("plan.json", "");
        String rawGameJs = arts.containsKey("gameJsLogicSection") && !arts.get("gameJsLogicSection").isBlank()
                ? arts.get("gameJsLogicSection")
                : arts.getOrDefault("game.js", "");
        String gameJs = truncateGameJs(rawGameJs);

        String systemPrompt = promptRegistry.getSystemPrompt("cto-feedback");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("cto-feedback"),
                Map.of("feedback", feedback, "plan", plan, "gameJs", gameJs)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());
        log.info("[CTO] 피드백 분석 완료 - projectId={}", context.getProjectId());

        String message = extractChatMessage(response.content(), "피드백 분석 완료! 수정 지시서 넘길게요.");
        return AgentResult.success(
                Map.of("debug-instruction.json", response.content()),
                message,
                response.inputTokens(), response.outputTokens()
        );
    }

    public AgentResult executeDebug(AgentContext context) {
        log.info("[CTO] 디버그 분석 시작 - projectId={}, attempt={}", context.getProjectId(), context.getDebugAttempt());

        String bugReport = context.getBugReport() != null ? context.getBugReport() : "";
        Map<String, String> arts = context.getPreviousArtifacts();
        // gameJsLogicSection(Game 클래스만) 우선 사용, 없으면 병합본 사용하되 크기 제한
        String rawGameJs = arts.containsKey("gameJsLogicSection") && !arts.get("gameJsLogicSection").isBlank()
                ? arts.get("gameJsLogicSection")
                : arts.getOrDefault("game.js", "");
        String gameJs = truncateGameJs(rawGameJs);
        String indexHtml = arts.getOrDefault("index.html", "");

        String systemPrompt = promptRegistry.getSystemPrompt("cto-debug");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("cto-debug"),
                Map.of("bugReport", bugReport, "gameJs", gameJs, "indexHtml", indexHtml)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());

        String message = extractChatMessage(response.content(), "버그 원인 파악 완료! 수정 지시서 확인해주세요.");
        return AgentResult.success(
                Map.of("debug-instruction.json", response.content()),
                message,
                response.inputTokens(), response.outputTokens()
        );
    }

    private String extractChatMessage(String json, String fallback) {
        com.fasterxml.jackson.databind.JsonNode node = com.noriter.util.JsonParser.parse(json);
        if (node != null && node.has("chatMessage") && !node.get("chatMessage").asText().isBlank()) {
            return node.get("chatMessage").asText();
        }
        return fallback;
    }

    /**
     * game.js가 너무 길면 앞 15000자 + 생략 안내 + 뒤 15000자로 대체.
     * HTTP 400 및 타임아웃 방지.
     */
    private String truncateGameJs(String gameJs) {
        if (gameJs.length() <= MAX_GAME_JS_CHARS) return gameJs;
        int half = MAX_GAME_JS_CHARS / 2;
        String head = gameJs.substring(0, half);
        String tail = gameJs.substring(gameJs.length() - half);
        log.warn("[CTO] game.js 크기 초과 ({}자) — 앞뒤 {}자씩 발췌하여 전달", gameJs.length(), half);
        return head + "\n\n// ... (중략 — 코드 크기 초과로 생략) ...\n\n" + tail;
    }
}
