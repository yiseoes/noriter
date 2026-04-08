package com.noriter.agent.impl;

import com.noriter.agent.core.*;
import com.noriter.agent.prompt.PromptRegistry;
import com.noriter.agent.prompt.PromptTemplate;
import com.noriter.domain.enums.AgentRole;
import com.fasterxml.jackson.databind.JsonNode;
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
public class QaAgent implements BaseAgent {

    private final ClaudeApiClient claudeApiClient;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentRole getRole() {
        return AgentRole.QA;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[QA팀] 코드 검증 시작 - projectId={}, debugAttempt={}",
                context.getProjectId(), context.getDebugAttempt());

        boolean isRetest = context.getDebugAttempt() > 0;
        String promptId = isRetest ? "qa-retest" : "qa-main";

        String plan = context.getPreviousArtifacts().getOrDefault("plan.json", "");
        String indexHtml = context.getPreviousArtifacts().getOrDefault("index.html", "");
        String styleCss = context.getPreviousArtifacts().getOrDefault("style.css", "");
        String gameJs = context.getPreviousArtifacts().getOrDefault("game.js", "");

        String systemPrompt = promptRegistry.getSystemPrompt(promptId);
        String userPrompt;

        if (isRetest) {
            String previousReport = context.getPreviousArtifacts().getOrDefault("test-report.json", "");
            userPrompt = PromptTemplate.render(
                    promptRegistry.getUserPrompt(promptId),
                    Map.of("previousReport", previousReport, "indexHtml", indexHtml,
                            "styleCss", styleCss, "gameJs", gameJs)
            );
        } else {
            userPrompt = PromptTemplate.render(
                    promptRegistry.getUserPrompt(promptId),
                    Map.of("plan", plan, "indexHtml", indexHtml,
                            "styleCss", styleCss, "gameJs", gameJs)
            );
        }

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());
        String report = response.content();

        // result 필드 정확 파싱으로 PASS/FAIL 판정 (설계서 08_프롬프트 §14 기준)
        boolean passed = false;
        JsonNode reportNode = JsonParser.parse(report);
        if (reportNode != null && reportNode.has("result")) {
            passed = "PASS".equals(reportNode.get("result").asText());
        }

        log.info("[QA팀] 코드 검증 완료 - projectId={}, 결과={}", context.getProjectId(), passed ? "PASS" : "FAIL");

        if (passed) {
            return AgentResult.success(
                    Map.of("test-report.json", report),
                    "모든 테스트를 통과했습니다. 출시 준비 완료.",
                    response.inputTokens(), response.outputTokens()
            );
        } else {
            return AgentResult.needsReview(
                    Map.of("test-report.json", report),
                    "테스트에서 버그가 발견되었습니다. 디버깅이 필요합니다.",
                    response.inputTokens(), response.outputTokens()
            );
        }
    }
}
