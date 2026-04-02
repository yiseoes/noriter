package com.noriter.repository;

import com.noriter.domain.LogEntry;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<LogEntry, String> {

    Page<LogEntry> findByProjectIdOrderByTimestampDesc(String projectId, Pageable pageable);

    Page<LogEntry> findByProjectIdAndLevelOrderByTimestampDesc(String projectId, LogLevel level, Pageable pageable);

    Page<LogEntry> findByProjectIdAndAgentRoleOrderByTimestampDesc(String projectId, AgentRole agentRole, Pageable pageable);

    Page<LogEntry> findByProjectIdAndLevelAndAgentRoleOrderByTimestampDesc(
            String projectId, LogLevel level, AgentRole agentRole, Pageable pageable);
}
