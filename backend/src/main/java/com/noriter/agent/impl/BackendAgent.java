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

@Log4j2
@Component
@RequiredArgsConstructor
public class BackendAgent implements BaseAgent {

    private final ClaudeApiClient claudeApiClient;
    private final PromptRegistry promptRegistry;

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

        String systemPrompt = promptRegistry.getSystemPrompt("back-main");
        String userPrompt = PromptTemplate.render(
                promptRegistry.getUserPrompt("back-main"),
                Map.of("plan", plan, "architecture", architecture, "design", design, "renderCode", renderCode)
        );

        ClaudeResponse response = claudeApiClient.sendPrompt(systemPrompt, userPrompt, getRole());

        // Claude 응답에서 gameJs 파싱
        String content = response.content();
        Map<String, String> parsed = JsonParser.parseAsMap(content);
        String gameLogic;
        if (parsed.containsKey("gameJs")) {
            gameLogic = parsed.get("gameJs");
        } else {
            log.warn("[백엔드팀] JSON 파싱 실패, 코드 추출 시도 - projectId={}", context.getProjectId());
            // JSON 래퍼에서 gameJs 값 수동 추출 시도
            String stripped = JsonParser.stripCodeBlock(content);
            if (stripped.contains("\"gameJs\"")) {
                int start = stripped.indexOf("\"gameJs\"");
                int codeStart = stripped.indexOf("\"", start + 8) + 1;
                int codeEnd = stripped.lastIndexOf("\"");
                if (codeStart > 0 && codeEnd > codeStart) {
                    gameLogic = stripped.substring(codeStart, codeEnd)
                            .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
                } else {
                    gameLogic = stripped;
                }
            } else {
                gameLogic = stripped;
            }
        }

        log.info("[백엔드팀] 게임 로직 구현 완료 - projectId={}", context.getProjectId());

        return AgentResult.success(
                Map.of("gameJsLogicSection", gameLogic),
                "게임 로직 구현 완료.",
                response.inputTokens(), response.outputTokens()
        );
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

        Map<String, String> parsed = JsonParser.parseAsMap(response.content());
        String fixedGameLogic = parsed.getOrDefault("gameJs", response.content());

        return AgentResult.success(
                Map.of("gameJsLogicSection", fixedGameLogic),
                "게임 로직 버그 수정 완료.",
                response.inputTokens(), response.outputTokens()
        );
    }
}
