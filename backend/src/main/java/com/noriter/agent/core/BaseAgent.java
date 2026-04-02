package com.noriter.agent.core;

import com.noriter.domain.enums.AgentRole;

/**
 * 에이전트 공통 인터페이스
 * 참조: 03_아키텍처 §3.1 BaseAgent
 */
public interface BaseAgent {

    AgentResult execute(AgentContext context);

    AgentRole getRole();

    default void cancel() {
        // 기본 구현: 아무것도 하지 않음, 필요 시 오버라이드
    }
}
