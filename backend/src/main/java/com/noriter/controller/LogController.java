package com.noriter.controller;

import com.noriter.controller.dto.response.*;
import com.noriter.domain.AgentMessageEntity;
import com.noriter.domain.AuditLog;
import com.noriter.domain.LogEntry;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.AuditEventType;
import com.noriter.domain.enums.LogLevel;
import com.noriter.repository.AgentMessageRepository;
import com.noriter.service.AuditService;
import com.noriter.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 로그 및 감사 API
 * 참조: 05_API §3 API-LOG-001~004 (4개 엔드포인트)
 */
@Log4j2
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;
    private final AuditService auditService;
    private final AgentMessageRepository agentMessageRepository;

    /**
     * API-LOG-001: 프로젝트 로그 조회
     * GET /api/projects/{id}/logs
     */
    @GetMapping("/projects/{id}/logs")
    public ResponseEntity<PageResponse<LogEntryResponse>> getProjectLogs(
            @PathVariable String id,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String agentRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("[API-LOG-001] 프로젝트 로그 조회 - projectId={}, level={}, agent={}, page={}",
                id, level, agentRole, page);

        LogLevel logLevel = level != null ? LogLevel.valueOf(level) : null;
        AgentRole role = agentRole != null ? AgentRole.valueOf(agentRole) : null;

        Page<LogEntry> logs = logService.getLogs(id, logLevel, role, PageRequest.of(page, size));
        List<LogEntryResponse> content = logs.getContent().stream()
                .map(LogEntryResponse::new).toList();

        log.info("[API-LOG-001] 로그 조회 완료 - projectId={}, 총 {}건", id, logs.getTotalElements());
        return ResponseEntity.ok(new PageResponse<>(logs, content));
    }

    /**
     * API-LOG-002: 에이전트 대화 로그 조회
     * GET /api/projects/{id}/messages
     */
    @GetMapping("/projects/{id}/messages")
    public ResponseEntity<Map<String, List<AgentMessageResponse>>> getAgentMessages(
            @PathVariable String id,
            @RequestParam(required = false) String agentRole) {
        log.info("[API-LOG-002] 에이전트 대화 조회 - projectId={}, agentFilter={}", id, agentRole);

        List<AgentMessageEntity> messages;
        if (agentRole != null) {
            messages = agentMessageRepository.findByProjectIdAndAgentRole(id, AgentRole.valueOf(agentRole));
        } else {
            messages = agentMessageRepository.findByProjectIdOrderByCreatedAtAsc(id);
        }

        List<AgentMessageResponse> content = messages.stream()
                .map(AgentMessageResponse::new).toList();

        log.info("[API-LOG-002] 대화 조회 완료 - projectId={}, {}건", id, content.size());
        return ResponseEntity.ok(Map.of("messages", content));
    }

    /**
     * API-LOG-003: 감사 로그 조회
     * GET /api/audit-logs
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("[API-LOG-003] 감사 로그 조회 - eventType={}, projectId={}, page={}", eventType, projectId, page);

        AuditEventType auditEventType = eventType != null ? AuditEventType.valueOf(eventType) : null;

        Page<AuditLog> auditLogs = auditService.getAuditLogs(
                auditEventType, projectId, startDate, endDate, PageRequest.of(page, size));
        List<AuditLogResponse> content = auditLogs.getContent().stream()
                .map(AuditLogResponse::new).toList();

        log.info("[API-LOG-003] 감사 로그 조회 완료 - 총 {}건", auditLogs.getTotalElements());
        return ResponseEntity.ok(new PageResponse<>(auditLogs, content));
    }

    /**
     * API-LOG-004: 에러 코드 상세 조회
     * GET /api/error-codes/{code}
     * 참조: 05_API §4 API-LOG-004 — 정적 매핑
     */
    @GetMapping("/error-codes/{code}")
    public ResponseEntity<ErrorCodeResponse> getErrorCodeDetail(@PathVariable String code) {
        log.info("[API-LOG-004] 에러 코드 상세 조회 - code={}", code);

        ErrorCodeResponse response = ErrorCodeRegistry.lookup(code);
        if (response == null) {
            log.warn("[API-LOG-004] 알 수 없는 에러 코드 - code={}", code);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }
}
