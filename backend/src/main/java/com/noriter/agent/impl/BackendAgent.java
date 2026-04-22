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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@Component
@RequiredArgsConstructor
public class BackendAgent implements BaseAgent {

    private final ClaudeApiClient claudeApiClient;
    private final PromptRegistry promptRegistry;

    private static final Pattern GAME_JS_PATTERN =
            Pattern.compile("===GAME_JS===\\s*```[a-z]*\\s*([\\s\\S]*?)```", Pattern.MULTILINE);

    @Override
    public AgentRole getRole() {
        return AgentRole.BACKEND;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[백엔드팀] 게임 로직 구현 시작 - projectId={}", context.getProjectId());

        String plan = context.getPreviousArtifacts().getOrDefault("plan.json", "");
        String architecture = context.getPreviousArtifacts().getOrDefault("architecture.json", "");
        String design = context.getPreviousArtifacts().getOrDefault("design.json", "");
        String renderCode = context.getPreviousArtifacts().getOrDefault("gameJsRenderSection", "");
        String contentData = context.getPreviousArtifacts().getOrDefault("content.json", "");

        String systemPrompt = promptRegistry.getSystemPrompt("back-main");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("back-main"),
                Map.of("plan", plan, "architecture", architecture, "design", design,
                       "renderCode", renderCode, "contentData", contentData)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());
        String gameLogic = parseGameJs(response.content());

        log.info("[백엔드팀] 게임 로직 구현 완료 - projectId={}", context.getProjectId());

        return AgentResult.success(
                Map.of("gameJsLogicSection", gameLogic),
                "게임 로직 구현 완료.",
                response.inputTokens(), response.outputTokens()
        );
    }

    /**
     * ===GAME_JS=== ```javascript ... ``` 블록 파싱.
     * 없으면 JSON { "gameJs": "..." } 형식 시도, 최후엔 stripCodeBlock.
     */
    private String parseGameJs(String content) {
        // 1. ===GAME_JS=== 섹션 블록
        Matcher matcher = GAME_JS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 2. JSON { "gameJs": "..." } 형식 — 구버전 응답 또는 모델이 JSON 형식으로 반환한 경우
        String stripped = JsonParser.stripCodeBlock(content);
        Map<String, String> jsonMap = JsonParser.parseAsMap(stripped);
        if (jsonMap.containsKey("gameJs") && !jsonMap.get("gameJs").isBlank()) {
            log.warn("[백엔드팀] GAME_JS 섹션 없음, JSON gameJs 필드에서 추출");
            return jsonMap.get("gameJs").trim();
        }

        log.warn("[백엔드팀] GAME_JS 섹션 없음, 코드블록 추출 시도");
        return stripped;
    }

    public AgentResult executeFix(AgentContext context) {
        log.info("[백엔드팀] 버그 수정 시작 - projectId={}", context.getProjectId());

        String ctoInstruction = context.getCtoInstruction() != null ? context.getCtoInstruction() : "";
        String bugReport = context.getBugReport() != null ? context.getBugReport() : "";
        String gameJs = context.getPreviousArtifacts().getOrDefault("game.js", "");

        String systemPrompt = promptRegistry.getSystemPrompt("back-fix");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("back-fix"),
                Map.of("ctoInstruction", ctoInstruction, "bugReport", bugReport, "gameJs", gameJs)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());
        String fixedLogic = parseGameJs(response.content());

        return AgentResult.success(
                Map.of("gameJsLogicSection", fixedLogic),
                "게임 로직 버그 수정 완료.",
                response.inputTokens(), response.outputTokens()
        );
    }
}
