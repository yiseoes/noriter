package com.noriter.agent.core;

import com.noriter.domain.enums.StageType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 에이전트 실행 컨텍스트
 * 참조: 03_아키텍처 §3.2 AgentContext
 */
@Getter
@Builder
public class AgentContext {

    private final String projectId;
    private final StageType stageType;
    private final String requirement;           // 사용자 요구사항 원문
    private final String genre;                 // 게임 장르 (nullable)
    private final Map<String, String> previousArtifacts;  // 이전 스테이지 산출물 (key=파일명, value=내용)
    private final String feedback;              // 수정 요청 (NT-PRJ-006, nullable)
    private final String ctoInstruction;        // CTO 디버깅 지시 (STAGE 6, nullable)
    private final String bugReport;             // 버그 리포트 JSON (STAGE 6, nullable)
    private final int debugAttempt;             // 현재 디버깅 시도 횟수 (0~3)
}
