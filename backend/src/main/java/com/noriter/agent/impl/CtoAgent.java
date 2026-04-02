package com.noriter.agent.impl;

import com.noriter.agent.core.*;
import com.noriter.domain.enums.AgentRole;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * CTO 에이전트 (AGT-CTO)
 * 참조: 04_에이전트 §2.1, 08_프롬프트 §6 PROMPT-CTO-MAIN
 * 담당: STAGE 2 — 기획서 → architecture.json
 */
@Log4j2
@Component
public class CtoAgent implements BaseAgent {

    @Override
    public AgentRole getRole() {
        return AgentRole.CTO;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[CTO] 기술 아키텍처 결정 시작 - projectId={}", context.getProjectId());

        // TODO: Claude API 호출 (PROMPT-CTO-MAIN)
        String dummyArch = """
                {
                  "gameType": "single-page",
                  "fileStructure": [
                    {"path": "index.html", "description": "메인 HTML"},
                    {"path": "style.css", "description": "스타일시트"},
                    {"path": "game.js", "description": "게임 로직"}
                  ],
                  "technology": {
                    "renderer": "Canvas2D",
                    "language": "JavaScript (ES6+)",
                    "styling": "CSS3"
                  },
                  "modules": [
                    {"name": "Game", "responsibility": "게임 루프, 상태 관리", "file": "game.js"},
                    {"name": "Player", "responsibility": "플레이어 로직", "file": "game.js"},
                    {"name": "Renderer", "responsibility": "렌더링", "file": "game.js"}
                  ],
                  "notes": "Canvas 2D API 기반 단순 구조"
                }
                """;

        log.info("[CTO] 기술 아키텍처 결정 완료 - projectId={}, renderer=Canvas2D", context.getProjectId());

        return AgentResult.success(
                Map.of("architecture.json", dummyArch),
                "아키텍처 결정 완료. Canvas 2D 기반으로 진행합니다.",
                400, 600
        );
    }
}
