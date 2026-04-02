package com.noriter.service;

import com.noriter.domain.AuditLog;
import com.noriter.domain.enums.AuditEventType;
import com.noriter.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(AuditEventType eventType, String projectId, String description, String detail) {
        log.debug("[감사 로그] 기록 - eventType={}, projectId={}, description={}",
                eventType, projectId, description);

        AuditLog auditLog = AuditLog.create(eventType, projectId, description, detail);
        auditLogRepository.save(auditLog);

        log.info("[감사 로그] 저장 완료 - id={}, eventType={}, description={}",
                auditLog.getId(), eventType, description);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(AuditEventType eventType, String projectId,
                                        LocalDateTime startDate, LocalDateTime endDate,
                                        Pageable pageable) {
        log.debug("[감사 로그 조회] 시작 - eventType={}, projectId={}, 기간={}~{}, page={}",
                eventType, projectId, startDate, endDate, pageable.getPageNumber());

        Page<AuditLog> result;

        if (eventType != null) {
            result = auditLogRepository.findByEventTypeOrderByTimestampDesc(eventType, pageable);
        } else if (projectId != null) {
            result = auditLogRepository.findByProjectIdOrderByTimestampDesc(projectId, pageable);
        } else if (startDate != null && endDate != null) {
            result = auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(
                    startDate, endDate, pageable);
        } else {
            result = auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        }

        log.debug("[감사 로그 조회] 완료 - 총 {}건 조회됨", result.getTotalElements());
        return result;
    }
}
