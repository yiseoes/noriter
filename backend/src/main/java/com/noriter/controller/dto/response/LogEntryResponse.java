package com.noriter.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.noriter.domain.LogEntry;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API-LOG-001: 로그 항목 응답
 * 참조: 05_API §4 API-LOG-001 응답
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogEntryResponse {

    private final String id;
    private final String level;
    private final String agentRole;
    private final String stage;
    private final String message;
    private final String errorCode;
    private final String stackTrace;
    private final Boolean resolved;
    private final String resolvedBy;
    private final LocalDateTime timestamp;

    public LogEntryResponse(LogEntry log) {
        this.id = log.getId();
        this.level = log.getLevel().name();
        this.agentRole = log.getAgentRole() != null ? log.getAgentRole().name() : null;
        this.stage = log.getStage() != null ? log.getStage().name() : null;
        this.message = log.getMessage();
        this.errorCode = log.getErrorCode();
        this.stackTrace = log.getStackTrace();
        this.resolved = log.getResolved();
        this.resolvedBy = log.getResolvedBy();
        this.timestamp = log.getTimestamp();
    }
}
