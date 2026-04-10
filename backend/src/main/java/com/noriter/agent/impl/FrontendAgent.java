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

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
@RequiredArgsConstructor
public class FrontendAgent implements BaseAgent {

    private final ClaudeApiClient claudeApiClient;
    private final PromptRegistry promptRegistry;

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

        // 1차 시도: 한번에 생성
        String systemPrompt = promptRegistry.getSystemPrompt("front-main");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("front-main"),
                Map.of("plan", plan, "architecture", architecture, "design", design)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());
        int totalInput = response.inputTokens();
        int totalOutput = response.outputTokens();

        Map<String, String> parsed = JsonParser.parseAsMap(response.content());
        Map<String, String> artifacts = new HashMap<>();

        // JSON 파싱 성공하면 그대로 사용
        if (!parsed.isEmpty() && parsed.containsKey("indexHtml")) {
            artifacts.put("index.html", parsed.getOrDefault("indexHtml", ""));
            artifacts.put("style.css", parsed.getOrDefault("styleCss", ""));
            artifacts.put("gameJsRenderSection", parsed.getOrDefault("renderJs", ""));
            log.info("[프론트팀] 통합 생성 성공 - projectId={}", context.getProjectId());
        } else {
            // JSON 파싱 실패 (토큰 초과로 잘림) → 분할 생성
            log.warn("[프론트팀] 통합 생성 실패, 분할 생성 전환 - projectId={}", context.getProjectId());
            Map<String, String> baseContext = Map.of("plan", plan, "architecture", architecture, "design", design);

            artifacts.put("index.html", generateSingleFile(baseContext, "index.html",
                    "HTML 파일만 생성해주세요. 완성된 index.html 코드만 반환하세요. JSON 없이 순수 HTML 코드만."));
            totalInput += 500; totalOutput += 2000;

            artifacts.put("style.css", generateSingleFile(baseContext, "style.css",
                    "CSS 파일만 생성해주세요. 완성된 style.css 코드만 반환하세요. JSON 없이 순수 CSS 코드만."));
            totalInput += 500; totalOutput += 2000;

            artifacts.put("gameJsRenderSection", generateSingleFile(baseContext, "renderJs",
                    "렌더링 JavaScript 코드만 생성해주세요. Renderer 클래스를 포함한 순수 JS 코드만 반환하세요. JSON 없이."));
            totalInput += 500; totalOutput += 4000;
        }

        log.info("[프론트팀] HTML/CSS/렌더링 구현 완료 - projectId={}", context.getProjectId());

        return AgentResult.success(artifacts, "프론트엔드 구현 완료.", totalInput, totalOutput);
    }

    private String generateSingleFile(Map<String, String> baseContext, String fileType, String instruction) {
        log.info("[프론트팀] 분할 생성 - fileType={}", fileType);

        String systemPrompt = "You are a frontend game developer. " + instruction + " Do NOT wrap in markdown code blocks.";
        String userPrompt = PromptTemplate.render(
                "게임 기획서:\n{{plan}}\n\n기술 아키텍처:\n{{architecture}}\n\n디자인 명세:\n{{design}}\n\n" + instruction,
                baseContext
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());
        String content = response.content();

        // 마크다운 코드블록 제거
        content = JsonParser.stripCodeBlock(content);
        return content;
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

        Map<String, String> parsed = JsonParser.parseAsMap(response.content());
        Map<String, String> artifacts = new HashMap<>();
        artifacts.put("index.html", parsed.getOrDefault("indexHtml", indexHtml));
        artifacts.put("style.css", parsed.getOrDefault("styleCss", styleCss));
        artifacts.put("gameJsRenderSection", parsed.getOrDefault("renderJs", renderCode));

        return AgentResult.success(
                artifacts, "프론트엔드 버그 수정 완료.",
                response.inputTokens(), response.outputTokens()
        );
    }
}
