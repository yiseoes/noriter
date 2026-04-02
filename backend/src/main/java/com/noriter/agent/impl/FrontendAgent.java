package com.noriter.agent.impl;

import com.noriter.agent.core.*;
import com.noriter.domain.enums.AgentRole;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 프론트엔드팀 에이전트 (AGT-FRONT)
 * 참조: 04_에이전트 §2.4, 08_프롬프트 §10 PROMPT-FRONT-MAIN
 * 담당: STAGE 4 (병렬) — index.html, style.css, gameJsRenderSection
 */
@Log4j2
@Component
public class FrontendAgent implements BaseAgent {

    @Override
    public AgentRole getRole() {
        return AgentRole.FRONTEND;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[프론트팀] HTML/CSS/렌더링 구현 시작 - projectId={}", context.getProjectId());

        // TODO: Claude API 호출 (PROMPT-FRONT-MAIN)
        String indexHtml = """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <title>미니게임</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <canvas id="gameCanvas" width="800" height="600"></canvas>
                    <script src="game.js"></script>
                </body>
                </html>
                """;

        String styleCss = """
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { display: flex; justify-content: center; align-items: center;
                       min-height: 100vh; background: #0f3460; }
                canvas { border: 2px solid #e94560; }
                """;

        String renderSection = """
                // 렌더링 코드 (프론트팀 담당)
                class Renderer {
                    constructor(canvas) {
                        this.canvas = canvas;
                        this.ctx = canvas.getContext('2d');
                    }
                    clear() { this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height); }
                    drawPlayer(player) {
                        this.ctx.fillStyle = '#e94560';
                        this.ctx.beginPath();
                        this.ctx.arc(player.x, player.y, 15, 0, Math.PI * 2);
                        this.ctx.fill();
                    }
                }
                """;

        log.info("[프론트팀] HTML/CSS/렌더링 구현 완료 - projectId={}", context.getProjectId());

        return AgentResult.success(
                Map.of("index.html", indexHtml, "style.css", styleCss, "gameJsRenderSection", renderSection),
                "프론트엔드 구현 완료. 백엔드 코드와 통합 대기.",
                600, 1200
        );
    }
}
