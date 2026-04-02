package com.noriter.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.noriter.domain.AuditLog;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API-LOG-003: 감사 로그 항목 응답
 * 참조: 05_API §4 API-LOG-003 응답
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogResponse {

    private final String id;
    private final String eventType;
    private final String projectId;
    private final String description;

    @JsonRawValue
    private final String detail;

    private final LocalDateTime timestamp;

    public AuditLogResponse(AuditLog log) {
        this.id = log.getId();
        this.eventType = log.getEventType().name();
        this.projectId = log.getProjectId();
        this.description = log.getDescription();
        this.detail = log.getDetail();
        this.timestamp = log.getTimestamp();
    }
}
