package com.noriter.repository;

import com.noriter.domain.AuditLog;
import com.noriter.domain.enums.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<AuditLog> findByEventTypeOrderByTimestampDesc(AuditEventType eventType, Pageable pageable);

    Page<AuditLog> findByProjectIdOrderByTimestampDesc(String projectId, Pageable pageable);

    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime start, LocalDateTime end, Pageable pageable);
}
