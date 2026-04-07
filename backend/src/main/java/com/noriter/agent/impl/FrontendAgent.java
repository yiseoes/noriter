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

        String systemPrompt = promptRegistry.getSystemPrompt("front-main");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("front-main"),
                Map.of("plan", plan, "architecture", architecture, "design", design)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());

        // Claude 응답에서 indexHtml, styleCss, renderJs 파싱
        Map<String, String> parsed = JsonParser.parseAsMap(response.content());
        Map<String, String> artifacts = new HashMap<>();
        artifacts.put("index.html", parsed.getOrDefault("indexHtml", response.content()));
        artifacts.put("style.css", parsed.getOrDefault("styleCss", ""));
        artifacts.put("gameJsRenderSection", parsed.getOrDefault("renderJs", ""));

        log.info("[프론트팀] HTML/CSS/렌더링 구현 완료 - projectId={}", context.getProjectId());

        return AgentResult.success(
                artifacts,
                "프론트엔드 구현 완료.",
                response.inputTokens(), response.outputTokens()
        );
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
        artifacts.put("index.html", parsed.getOrDefault("indexHtml", ""));
        artifacts.put("style.css", parsed.getOrDefault("styleCss", ""));
        artifacts.put("gameJsRenderSection", parsed.getOrDefault("renderJs", ""));

        return AgentResult.success(
                artifacts,
                "프론트엔드 버그 수정 완료.",
                response.inputTokens(), response.outputTokens()
        );
    }
}
