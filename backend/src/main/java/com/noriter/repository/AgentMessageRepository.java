package com.noriter.repository;

import com.noriter.domain.AgentMessageEntity;
import com.noriter.domain.enums.AgentRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgentMessageRepository extends JpaRepository<AgentMessageEntity, String> {

    List<AgentMessageEntity> findByProjectIdOrderByCreatedAtAsc(String projectId);

    @Query("SELECT m FROM AgentMessageEntity m WHERE m.projectId = :projectId " +
           "AND (m.fromAgent = :agentRole OR m.toAgent = :agentRole) ORDER BY m.createdAt ASC")
    List<AgentMessageEntity> findByProjectIdAndAgentRole(
            @Param("projectId") String projectId, @Param("agentRole") AgentRole agentRole);
}
