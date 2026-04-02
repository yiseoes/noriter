package com.noriter.domain;

import com.noriter.domain.enums.AuditEventType;
import com.noriter.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @Column(length = 20)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AuditEventType eventType;

    @Column(name = "project_id", length = 20)
    private String projectId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(columnDefinition = "JSON")
    private String detail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public static AuditLog create(AuditEventType eventType, String projectId,
                                   String description, String detail) {
        AuditLog log = new AuditLog();
        log.id = IdGenerator.generateAuditId();
        log.eventType = eventType;
        log.projectId = projectId;
        log.description = description;
        log.detail = detail;
        log.timestamp = LocalDateTime.now();
        return log;
    }
}
