package com.noriter.agent.impl;

import com.noriter.agent.core.*;
import com.noriter.domain.enums.AgentRole;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 백엔드팀 에이전트 (AGT-BACK)
 * 참조: 04_에이전트 §2.5, 08_프롬프트 §12 PROMPT-BACK-MAIN
 * 담당: STAGE 4 (병렬) — gameJsLogicSection
 */
@Log4j2
@Component
public class BackendAgent implements BaseAgent {

    @Override
    public AgentRole getRole() {
        return AgentRole.BACKEND;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[백엔드팀] 게임 로직 구현 시작 - projectId={}", context.getProjectId());

        // TODO: Claude API 호출 (PROMPT-BACK-MAIN)
        String logicSection = """
                // 게임 로직 (백엔드팀 담당)
                class Game {
                    constructor() {
                        this.state = 'TITLE';
                        this.score = 0;
                        this.player = { x: 400, y: 300, hp: 100 };
                        this.objects = [];
                        this.canvas = document.getElementById('gameCanvas');
                        this.renderer = new Renderer(this.canvas);
                        this.lastTime = 0;
                        this.gameLoop = this.gameLoop.bind(this);
                        requestAnimationFrame(this.gameLoop);
                    }
                    gameLoop(timestamp) {
                        const deltaTime = (timestamp - this.lastTime) / 1000;
                        this.lastTime = timestamp;
                        this.update(deltaTime);
                        this.render();
                        requestAnimationFrame(this.gameLoop);
                    }
                    update(dt) {
                        if (this.state !== 'PLAYING') return;
                        // TODO: 게임 오브젝트 업데이트
                    }
                    render() {
                        this.renderer.clear();
                        if (this.state === 'PLAYING') {
                            this.renderer.drawPlayer(this.player);
                        }
                    }
                }
                """;

        log.info("[백엔드팀] 게임 로직 구현 완료 - projectId={}", context.getProjectId());

        return AgentResult.success(
                Map.of("gameJsLogicSection", logicSection),
                "게임 로직 구현 완료. 프론트엔드 코드와 통합 준비.",
                500, 1100
        );
    }
}
