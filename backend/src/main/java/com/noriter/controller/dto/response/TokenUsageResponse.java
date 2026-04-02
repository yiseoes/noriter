package com.noriter.controller.dto.response;

import com.noriter.domain.enums.AgentRole;
import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * API-PRJ-003 상세 응답 내 tokenUsage 객체
 * 참조: 05_API §4 API-PRJ-003 tokenUsage
 */
@Getter
public class TokenUsageResponse {

    private final long total;
    private final Map<String, Long> byAgent;

    public TokenUsageResponse(long total, Map<AgentRole, Long> byAgent) {
        this.total = total;
        this.byAgent = byAgent.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue
                ));
    }
}
