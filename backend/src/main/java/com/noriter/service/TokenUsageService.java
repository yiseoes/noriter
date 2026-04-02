package com.noriter.service;

import com.noriter.domain.TokenUsage;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.StageType;
import com.noriter.repository.TokenUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class TokenUsageService {

    private final TokenUsageRepository tokenUsageRepository;

    @Transactional
    public TokenUsage record(String projectId, AgentRole agentRole, StageType stage,
                              int inputTokens, int outputTokens) {
        log.debug("[토큰 사용량 기록] projectId={}, agent={}, stage={}, input={}, output={}",
                projectId, agentRole, stage, inputTokens, outputTokens);

        TokenUsage usage = TokenUsage.create(projectId, agentRole, stage, inputTokens, outputTokens);
        tokenUsageRepository.save(usage);

        log.info("[토큰 사용량 기록] 저장 완료 - projectId={}, agent={}, 총 {}토큰",
                projectId, agentRole, usage.getTotalTokens());
        return usage;
    }

    @Transactional(readOnly = true)
    public long getTotalTokens(String projectId) {
        long total = tokenUsageRepository.sumTotalTokensByProjectId(projectId);
        log.debug("[토큰 사용량 조회] projectId={}, 전체={}토큰", projectId, total);
        return total;
    }

    @Transactional(readOnly = true)
    public Map<AgentRole, Long> getTokensByAgent(String projectId) {
        log.debug("[에이전트별 토큰 조회] projectId={}", projectId);

        List<Object[]> results = tokenUsageRepository.sumTokensByProjectIdGroupByAgent(projectId);
        Map<AgentRole, Long> tokenMap = new HashMap<>();

        for (Object[] row : results) {
            AgentRole role = (AgentRole) row[0];
            Long tokens = (Long) row[1];
            tokenMap.put(role, tokens);
            log.debug("[에이전트별 토큰 조회] {} = {}토큰", role, tokens);
        }

        return tokenMap;
    }
}
