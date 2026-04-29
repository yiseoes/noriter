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

    // MESSAGE 섹션 파싱
    private static final Pattern MESSAGE_PATTERN =
            Pattern.compile("===MESSAGE===\\s*([\\s\\S]*?)===END_MESSAGE===", Pattern.MULTILINE);

    // 신규 포맷: ===GAME_JS=== ... ===END_GAME_JS===
    private static final Pattern GAME_JS_END_PATTERN =
            Pattern.compile("===GAME_JS===\\s*([\\s\\S]*?)===END_GAME_JS===", Pattern.MULTILINE);
    // 구 포맷 (폴백): ===GAME_JS=== ```javascript ... ```
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

        String message = extractMessage(response.content(), "게임 로직 완성! 렌더러랑 연결해서 QA 돌려주세요.");
        return AgentResult.success(
                Map.of("gameJsLogicSection", gameLogic),
                message,
                response.inputTokens(), response.outputTokens()
        );
    }

    /**
     * ===GAME_JS=== ... ===END_GAME_JS=== 블록 파싱 (신규 포맷 우선).
     * 구 포맷(코드펜스), JSON 형식 순으로 폴백.
     */
    private String parseGameJs(String content) {
        // 1. 신규 포맷: ===GAME_JS=== ... ===END_GAME_JS===
        Matcher endMatcher = GAME_JS_END_PATTERN.matcher(content);
        if (endMatcher.find()) {
            return endMatcher.group(1).trim();
        }

        // 2. 구 포맷: ===GAME_JS=== ```javascript ... ``` (닫는 ``` 있는 경우)
        Matcher matcher = GAME_JS_PATTERN.matcher(content);
        if (matcher.find()) {
            log.warn("[백엔드팀] 구 포맷(코드펜스) 감지 — 신규 포맷(===END_GAME_JS===)으로 전환 권고");
            return matcher.group(1).trim();
        }

        // 3. ===GAME_JS=== 있지만 종료 마커 없는 경우 (마커 이후 내용 추출)
        int markerIdx = content.indexOf("===GAME_JS===");
        if (markerIdx >= 0) {
            String afterMarker = content.substring(markerIdx + "===GAME_JS===".length()).trim();
            String extracted = JsonParser.stripCodeBlock(afterMarker);
            if (!extracted.isBlank()) {
                log.warn("[백엔드팀] GAME_JS 종료 마커 없음 — 마커 이후 내용으로 추출");
                return extracted;
            }
        }

        // 4. JSON { "gameJs": "..." } 형식 폴백
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
        // game.js(병합본) 대신 Game 클래스 섹션만 전달해 프롬프트 크기 절감
        Map<String, String> arts = context.getPreviousArtifacts();
        String gameJs = arts.containsKey("gameJsLogicSection") && !arts.get("gameJsLogicSection").isBlank()
                ? arts.get("gameJsLogicSection")
                : arts.getOrDefault("game.js", "");

        String systemPrompt = promptRegistry.getSystemPrompt("back-fix");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("back-fix"),
                Map.of("ctoInstruction", ctoInstruction, "bugReport", bugReport, "gameJs", gameJs)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());
        String fixedLogic = parseGameJs(response.content());

        String message = extractMessage(response.content(), "게임 로직 버그 수정 완료! 다시 QA 돌려봐주세요.");
        return AgentResult.success(
                Map.of("gameJsLogicSection", fixedLogic),
                message,
                response.inputTokens(), response.outputTokens()
        );
    }

    private String extractMessage(String content, String fallback) {
        Matcher m = MESSAGE_PATTERN.matcher(content);
        if (m.find()) {
            String msg = m.group(1).trim();
            if (!msg.isBlank()) return msg;
        }
        return fallback;
    }
}
