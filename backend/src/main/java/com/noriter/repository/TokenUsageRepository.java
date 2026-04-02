package com.noriter.repository;

import com.noriter.domain.TokenUsage;
import com.noriter.domain.enums.AgentRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TokenUsageRepository extends JpaRepository<TokenUsage, String> {

    List<TokenUsage> findByProjectId(String projectId);

    @Query("SELECT COALESCE(SUM(t.inputTokens + t.outputTokens), 0) FROM TokenUsage t WHERE t.projectId = :projectId")
    Long sumTotalTokensByProjectId(@Param("projectId") String projectId);

    @Query("SELECT t.agentRole, COALESCE(SUM(t.inputTokens + t.outputTokens), 0) " +
           "FROM TokenUsage t WHERE t.projectId = :projectId GROUP BY t.agentRole")
    List<Object[]> sumTokensByProjectIdGroupByAgent(@Param("projectId") String projectId);
}
