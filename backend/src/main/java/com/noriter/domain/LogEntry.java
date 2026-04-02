package com.noriter.domain;

import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.LogLevel;
import com.noriter.domain.enums.StageType;
import com.noriter.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_entry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LogEntry {

    @Id
    @Column(length = 20)
    private String id;

    @Column(name = "project_id", nullable = false, length = 20)
    private String projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LogLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_role", length = 20)
    private AgentRole agentRole;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StageType stage;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "error_code", length = 20)
    private String errorCode;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column
    private Boolean resolved;

    @Column(name = "resolved_by", length = 20)
    private String resolvedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public static LogEntry create(String projectId, LogLevel level, AgentRole agentRole,
                                   StageType stage, String message) {
        LogEntry log = new LogEntry();
        log.id = IdGenerator.generateLogId();
        log.projectId = projectId;
        log.level = level;
        log.agentRole = agentRole;
        log.stage = stage;
        log.message = message;
        log.timestamp = LocalDateTime.now();
        return log;
    }

    public static LogEntry createError(String projectId, AgentRole agentRole, StageType stage,
                                        String message, String errorCode, String stackTrace) {
        LogEntry log = create(projectId, LogLevel.ERROR, agentRole, stage, message);
        log.errorCode = errorCode;
        log.stackTrace = stackTrace;
        log.resolved = false;
        return log;
    }

    public void markResolved(String resolvedBy) {
        this.resolved = true;
        this.resolvedBy = resolvedBy;
    }
}
