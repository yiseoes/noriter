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

    private String extractChatMessage(String json) {
        com.fasterxml.jackson.databind.JsonNode node = JsonParser.parse(json);
        if (node != null && node.has("chatMessage") && !node.get("chatMessage").asText().isBlank()) {
            return node.get("chatMessage").asText();
        }
        return null;
    }

    /**
     * QA 실패 시 유저가 읽기 쉬운 버그 리포트 메시지 생성
     */
    private String buildFailureMessage(JsonNode reportNode, String fallbackMessage) {
        if (reportNode == null) return fallbackMessage;

        StringBuilder sb = new StringBuilder();

        // 요약
        String summary = reportNode.has("summary") ? reportNode.get("summary").asText() : null;
        int failed = reportNode.has("testsFailed") ? reportNode.get("testsFailed").asInt() : 0;
        sb.append("QA 검증 결과: FAIL");
        if (failed > 0) sb.append(" (").append(failed).append("건 실패)");
        if (summary != null && !summary.isBlank()) sb.append("\n").append(summary);

        // CRITICAL/HIGH 버그만 나열
        JsonNode bugs = reportNode.has("bugs") ? reportNode.get("bugs") : null;
        if (bugs != null && bugs.isArray()) {
            for (JsonNode bug : bugs) {
                String severity = bug.has("severity") ? bug.get("severity").asText() : "";
                if (!"CRITICAL".equals(severity) && !"HIGH".equals(severity)) continue;

                String icon = "CRITICAL".equals(severity) ? "🔴 CRITICAL" : "🟠 HIGH";
                String category = bug.has("category") ? bug.get("category").asText() : "";
                String desc = bug.has("description") ? bug.get("description").asText() : "";
                String location = bug.has("location") ? bug.get("location").asText() : "";
                String fix = bug.has("suggestedFix") ? bug.get("suggestedFix").asText() : "";

                sb.append("\n\n").append(icon).append(" [").append(category).append("]");
                if (!location.isBlank()) sb.append(" — ").append(location);
                if (!desc.isBlank()) sb.append("\n→ ").append(desc);
                if (!fix.isBlank()) sb.append("\n→ 수정: ").append(fix);
            }
        }

        return sb.toString();
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

        String chatMessage = extractChatMessage(report);
        if (passed) {
            String message = (chatMessage != null) ? chatMessage : "전 항목 통과! 출시해도 됩니다!";
            return AgentResult.success(
                    Map.of("test-report.json", report),
                    message,
                    response.inputTokens(), response.outputTokens()
            );
        } else {
            String message = buildFailureMessage(reportNode, (chatMessage != null) ? chatMessage : "버그 발견했어요. CTO님, 수정 지시 부탁드려요.");
            return AgentResult.needsReview(
                    Map.of("test-report.json", report),
                    message,
                    response.inputTokens(), response.outputTokens()
            );
        }
    }
}
