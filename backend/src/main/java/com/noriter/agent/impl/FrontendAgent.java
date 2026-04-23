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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@Component
@RequiredArgsConstructor
public class FrontendAgent implements BaseAgent {

    private final ClaudeApiClient claudeApiClient;
    private final PromptRegistry promptRegistry;

    // 신규 포맷: ===SECTION=== ... ===END_SECTION===
    private static final Pattern SECTION_END_PATTERN =
            Pattern.compile("===([A-Z_]+)===\\s*([\\s\\S]*?)===END_\\1===", Pattern.MULTILINE);
    // 구 포맷 (폴백): ===SECTION=== ```lang ... ```
    private static final Pattern SECTION_PATTERN =
            Pattern.compile("===([A-Z_]+)===\\s*```[a-z]*\\s*([\\s\\S]*?)```", Pattern.MULTILINE);

    @Override
    public AgentRole getRole() {
        return AgentRole.FRONTEND;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[프론트팀] HTML/CSS/렌더링 구현 시작 - projectId={}", context.getProjectId());

        String plan = context.getPreviousArtifacts().getOrDefault("plan.json", "");
        String architecture = context.getPreviousArtifacts().getOrDefault("architecture.json", "");
        String design = context.getPreviousArtifacts().getOrDefault("design.json", "");

        String systemPrompt = promptRegistry.getSystemPrompt("front-main");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("front-main"),
                Map.of("plan", plan, "architecture", architecture, "design", design)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());
        String content = response.content();

        Map<String, String> artifacts = parseSectionBlocks(content);

        // 섹션 블록 파싱 실패 시 분할 생성으로 fallback
        if (!artifacts.containsKey("gameJsRenderSection") || artifacts.get("gameJsRenderSection").isBlank()) {
            log.warn("[프론트팀] 섹션 파싱 실패, 분할 생성 전환 - projectId={}", context.getProjectId());
            Map<String, String> baseContext = Map.of("plan", plan, "architecture", architecture, "design", design);

            if (!artifacts.containsKey("index.html") || artifacts.get("index.html").isBlank()) {
                artifacts.put("index.html", generateSingleFile(baseContext,
                        "index.html만 생성하세요. ```html ... ``` 코드블록으로 반환. Game 클래스나 JS 로직 없이 순수 HTML만."));
            }
            if (!artifacts.containsKey("style.css") || artifacts.get("style.css").isBlank()) {
                artifacts.put("style.css", generateSingleFile(baseContext,
                        "style.css만 생성하세요. ```css ... ``` 코드블록으로 반환. 순수 CSS만."));
            }
            artifacts.put("gameJsRenderSection", generateSingleFile(baseContext,
                    "Renderer 클래스만 생성하세요. ```javascript ... ``` 코드블록으로 반환. " +
                    "architecture의 gameInterface.rendererClass.publicMethods를 모두 구현하세요. " +
                    "Game 클래스, 초기화 코드, 게임 루프 절대 포함 금지."));
        }

        log.info("[프론트팀] HTML/CSS/렌더링 구현 완료 - projectId={}", context.getProjectId());
        return AgentResult.success(artifacts, "프론트엔드 구현 완료.", response.inputTokens(), response.outputTokens());
    }

    /**
     * 섹션 블록 파싱.
     * 신규 포맷(===END_SECTION===) → 구 포맷(코드펜스) → 마커 위치 기반 분리 순으로 시도.
     */
    private static final Pattern SECTION_MARKER_PATTERN =
            Pattern.compile("===([A-Z_]+)===");

    private Map<String, String> parseSectionBlocks(String content) {
        Map<String, String> result = new HashMap<>();

        // 1. 신규 포맷: ===SECTION=== ... ===END_SECTION===
        Matcher endMatcher = SECTION_END_PATTERN.matcher(content);
        while (endMatcher.find()) {
            String section = endMatcher.group(1);
            // END_ 접두사가 붙은 종료 마커는 섹션명이 아니므로 제외
            if (section.startsWith("END_")) continue;
            String code = endMatcher.group(2).trim();
            mapSection(result, section, code);
        }
        if (!result.isEmpty()) return result;

        // 2. 구 포맷: ===SECTION=== ```lang ... ```
        Matcher matcher = SECTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String section = matcher.group(1);
            if (section.startsWith("END_")) continue;
            String code = matcher.group(2).trim();
            log.warn("[프론트팀] 구 포맷(코드펜스) 감지 — 신규 포맷(===END_SECTION===)으로 전환 권고");
            mapSection(result, section, code);
        }
        if (!result.isEmpty()) return result;

        // 3. 섹션 마커는 있지만 종료 마커 없는 경우 — 마커 위치 기반으로 분리 추출
        Matcher markerMatcher = SECTION_MARKER_PATTERN.matcher(content);
        List<int[]> positions = new ArrayList<>();
        List<String> sections = new ArrayList<>();
        while (markerMatcher.find()) {
            String name = markerMatcher.group(1);
            if (!name.startsWith("END_")) {
                positions.add(new int[]{markerMatcher.start(), markerMatcher.end()});
                sections.add(name);
            }
        }
        if (!positions.isEmpty()) {
            log.warn("[프론트팀] 섹션 종료 마커 없음 — 마커 위치 기반으로 분리 추출");
            for (int i = 0; i < sections.size(); i++) {
                int contentStart = positions.get(i)[1];
                int contentEnd = (i + 1 < positions.size()) ? positions.get(i + 1)[0] : content.length();
                String raw = content.substring(contentStart, contentEnd).trim();
                String code = JsonParser.stripCodeBlock(raw);
                mapSection(result, sections.get(i), code);
            }
        }
        if (!result.isEmpty()) return result;

        // 4. 섹션 자체가 없으면 전체를 코드블록으로 시도 (단일 파일 fallback)
        String stripped = JsonParser.stripCodeBlock(content);
        if (!stripped.isBlank()) {
            result.put("gameJsRenderSection", stripped);
        }
        return result;
    }

    private void mapSection(Map<String, String> result, String section, String code) {
        switch (section) {
            case "INDEX_HTML" -> result.put("index.html", code);
            case "STYLE_CSS"  -> result.put("style.css", code);
            case "RENDER_JS"  -> result.put("gameJsRenderSection", code);
        }
    }

    private String generateSingleFile(Map<String, String> baseContext, String instruction) {
        String systemPrompt = "You are a frontend game developer. " + instruction;
        String userPrompt = PromptTemplate.render(
                "게임 기획서:\n{{plan}}\n\n기술 아키텍처:\n{{architecture}}\n\n디자인 명세:\n{{design}}\n\n" + instruction,
                baseContext
        );
        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());
        return JsonParser.stripCodeBlock(response.content());
    }

    public AgentResult executeFix(AgentContext context) {
        log.info("[프론트팀] 버그 수정 시작 - projectId={}", context.getProjectId());

        String ctoInstruction = context.getCtoInstruction() != null ? context.getCtoInstruction() : "";
        String indexHtml = context.getPreviousArtifacts().getOrDefault("index.html", "");
        String styleCss = context.getPreviousArtifacts().getOrDefault("style.css", "");
        String renderCode = context.getPreviousArtifacts().getOrDefault("gameJsRenderSection", "");

        String systemPrompt = promptRegistry.getSystemPrompt("front-fix");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("front-fix"),
                Map.of("ctoInstruction", ctoInstruction, "indexHtml", indexHtml,
                        "styleCss", styleCss, "renderCode", renderCode)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());
        String content = response.content();

        Map<String, String> artifacts = parseSectionBlocks(content);

        // fallback: 기존 파일 유지
        artifacts.putIfAbsent("index.html", indexHtml);
        artifacts.putIfAbsent("style.css", styleCss);
        artifacts.putIfAbsent("gameJsRenderSection", renderCode);

        return AgentResult.success(artifacts, "프론트엔드 버그 수정 완료.",
                response.inputTokens(), response.outputTokens());
    }
}
