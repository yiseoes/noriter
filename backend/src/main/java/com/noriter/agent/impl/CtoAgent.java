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

        return AgentResult.success(
                Map.of("architecture.json", response.content()),
                "기술 아키텍처 설계를 완료했습니다.",
                response.inputTokens(), response.outputTokens()
        );
    }

    public AgentResult executeDebug(AgentContext context) {
        log.info("[CTO] 디버그 분석 시작 - projectId={}, attempt={}", context.getProjectId(), context.getDebugAttempt());

        String bugReport = context.getBugReport() != null ? context.getBugReport() : "";
        String gameJs = context.getPreviousArtifacts().getOrDefault("game.js", "");
        String indexHtml = context.getPreviousArtifacts().getOrDefault("index.html", "");

        String systemPrompt = promptRegistry.getSystemPrompt("cto-debug");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("cto-debug"),
                Map.of("bugReport", bugReport, "gameJs", gameJs, "indexHtml", indexHtml)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());

        return AgentResult.success(
                Map.of("debug-instruction.json", response.content()),
                "디버그 분석을 완료했습니다.",
                response.inputTokens(), response.outputTokens()
        );
    }
}
