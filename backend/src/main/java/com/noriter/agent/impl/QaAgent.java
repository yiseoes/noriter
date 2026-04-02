package com.noriter.agent.impl;

import com.noriter.agent.core.*;
import com.noriter.domain.enums.AgentRole;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * QA팀 에이전트 (AGT-QA)
 * 참조: 04_에이전트 §2.6, 08_프롬프트 §14 PROMPT-QA-MAIN
 * 담당: STAGE 5 — 코드 검증, test-report.json
 */
@Log4j2
@Component
public class QaAgent implements BaseAgent {

    @Override
    public AgentRole getRole() {
        return AgentRole.QA;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        log.info("[QA팀] 코드 검증 시작 - projectId={}, 디버깅 시도={}/3",
                context.getProjectId(), context.getDebugAttempt());

        // TODO: Claude API 호출 (PROMPT-QA-MAIN / PROMPT-QA-RETEST)
        String dummyReport = """
                {
                  "result": "PASS",
                  "summary": "모든 테스트를 통과했습니다.",
                  "testsRun": 8,
                  "testsPassed": 8,
                  "testsFailed": 0,
                  "bugs": [],
                  "codeQuality": {
                    "syntaxErrors": 0,
                    "logicIssues": [],
                    "performanceConcerns": [],
                    "securityIssues": []
                  },
                  "checklist": {
                    "htmlValid": true, "cssValid": true, "jsNoErrors": true,
                    "gameStartable": true, "controlsWorking": true,
                    "scoreTracking": true, "gameOverCondition": true, "allScreensPresent": true
                  }
                }
                """;

        // 더미에서는 항상 PASS — 실제 구현 시 Claude 응답의 result 필드로 판단
        boolean passed = true;

        log.info("[QA팀] 코드 검증 완료 - projectId={}, 결과={}", context.getProjectId(), passed ? "PASS" : "FAIL");

        if (passed) {
            return AgentResult.success(
                    Map.of("test-report.json", dummyReport),
                    "모든 테스트를 통과했습니다. 출시 준비 완료.",
                    400, 600
            );
        } else {
            return AgentResult.needsReview(
                    Map.of("test-report.json", dummyReport),
                    "테스트에서 버그가 발견되었습니다. 디버깅이 필요합니다.",
                    400, 600
            );
        }
    }
}
