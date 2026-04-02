package com.noriter.agent.impl;

import com.noriter.agent.core.*;
import com.noriter.domain.enums.AgentRole;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 기획팀 에이전트 (AGT-PLAN)
 * 참조: 04_에이전트 §2.2, 08_프롬프트 §5 PROMPT-PLAN-MAIN
 * 담당: STAGE 1 — 사용자 요구사항 → plan.json
 */
@Log4j2
@Component
public class PlanningAgent implements BaseAgent {

    @Override
    public AgentRole getRole() {
        return AgentRole.PLANNING;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[기획팀] 게임 기획서 작성 시작 - projectId={}, 요구사항 길이={}자",
                context.getProjectId(), context.getRequirement().length());

        // TODO: Claude API 호출 (PROMPT-PLAN-MAIN)
        // 현재는 더미 응답 반환
        log.info("[기획팀] Claude API 호출 준비 - model={}, temperature={}, maxTokens={}",
                AgentConfig.forRole(getRole()).getModel(),
                AgentConfig.forRole(getRole()).getTemperature(),
                AgentConfig.forRole(getRole()).getMaxTokens());

        String dummyPlan = generateDummyPlan(context.getRequirement());

        log.info("[기획팀] 게임 기획서 작성 완료 - projectId={}", context.getProjectId());

        return AgentResult.success(
                Map.of("plan.json", dummyPlan),
                "게임 기획서 작성을 완료했습니다. 검토 부탁드립니다.",
                500, 800
        );
    }

    private String generateDummyPlan(String requirement) {
        return """
                {
                  "gameName": "미니게임",
                  "genre": "액션",
                  "concept": "%s",
                  "rules": {
                    "objective": "높은 점수 달성",
                    "winCondition": "제한 시간 생존",
                    "loseCondition": "HP가 0",
                    "scoring": "적 처치당 10점"
                  },
                  "controls": {
                    "movement": "WASD 또는 방향키",
                    "attack": "자동",
                    "special": "스페이스바"
                  },
                  "gameObjects": [
                    {"name": "Player", "description": "플레이어 캐릭터"},
                    {"name": "Enemy", "description": "적 오브젝트"}
                  ],
                  "screens": [
                    {"name": "TitleScreen", "description": "타이틀 화면"},
                    {"name": "GameScreen", "description": "게임 화면"},
                    {"name": "GameOverScreen", "description": "게임오버 화면"}
                  ],
                  "difficulty": {
                    "progression": "시간에 따라 난이도 상승",
                    "levels": "1분마다 레벨업"
                  }
                }
                """.formatted(requirement.substring(0, Math.min(50, requirement.length())));
    }
}
