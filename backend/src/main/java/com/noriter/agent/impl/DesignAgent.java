package com.noriter.agent.impl;

import com.noriter.agent.core.*;
import com.noriter.domain.enums.AgentRole;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 디자인팀 에이전트 (AGT-DESIGN)
 * 참조: 04_에이전트 §2.3, 08_프롬프트 §9 PROMPT-DESIGN-MAIN
 * 담당: STAGE 3 — 기획서+아키텍처 → design.json
 */
@Log4j2
@Component
public class DesignAgent implements BaseAgent {

    @Override
    public AgentRole getRole() {
        return AgentRole.DESIGN;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[디자인팀] UI/UX 디자인 스펙 작성 시작 - projectId={}", context.getProjectId());

        // TODO: Claude API 호출 (PROMPT-DESIGN-MAIN)
        String dummyDesign = """
                {
                  "colorPalette": {
                    "primary": "#1a1a2e", "secondary": "#16213e",
                    "accent": "#e94560", "background": "#0f3460",
                    "text": "#ffffff", "success": "#00ff88", "danger": "#ff4444"
                  },
                  "typography": {
                    "fontFamily": "'Press Start 2P', monospace",
                    "fontCdn": "https://fonts.googleapis.com/css2?family=Press+Start+2P&display=swap",
                    "fontSize": {"title": "32px", "subtitle": "18px", "body": "14px", "score": "24px"}
                  },
                  "layout": {"canvasWidth": 800, "canvasHeight": 600, "uiElements": []},
                  "screens": {},
                  "sprites": {"style": "기하학적 도형"},
                  "effects": []
                }
                """;

        log.info("[디자인팀] 디자인 스펙 작성 완료 - projectId={}", context.getProjectId());

        return AgentResult.success(
                Map.of("design.json", dummyDesign),
                "디자인 스펙 작성 완료. 구현팀에 전달합니다.",
                450, 700
        );
    }
}
