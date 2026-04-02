package com.noriter.service;

import com.noriter.domain.LogEntry;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.LogLevel;
import com.noriter.domain.enums.StageType;
import com.noriter.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;

    @Transactional
    public LogEntry createLog(String projectId, LogLevel level, AgentRole agentRole,
                               StageType stage, String message) {
        log.debug("[로그 기록] projectId={}, level={}, agent={}, stage={}, message={}",
                projectId, level, agentRole, stage, message);

        LogEntry logEntry = LogEntry.create(projectId, level, agentRole, stage, message);
        logRepository.save(logEntry);
        return logEntry;
    }

    @Transactional
    public LogEntry createErrorLog(String projectId, AgentRole agentRole, StageType stage,
                                    String message, String errorCode, String stackTrace) {
        log.error("[에러 로그 기록] projectId={}, agent={}, stage={}, errorCode={}, message={}",
                projectId, agentRole, stage, errorCode, message);

        LogEntry logEntry = LogEntry.createError(projectId, agentRole, stage,
                message, errorCode, stackTrace);
        logRepository.save(logEntry);
        return logEntry;
    }

    @Transactional(readOnly = true)
    public Page<LogEntry> getLogs(String projectId, LogLevel level, AgentRole agentRole,
                                   Pageable pageable) {
        log.debug("[로그 조회] projectId={}, level={}, agent={}, page={}",
                projectId, level, agentRole, pageable.getPageNumber());

        Page<LogEntry> result;

        if (level != null && agentRole != null) {
            result = logRepository.findByProjectIdAndLevelAndAgentRoleOrderByTimestampDesc(
                    projectId, level, agentRole, pageable);
        } else if (level != null) {
            result = logRepository.findByProjectIdAndLevelOrderByTimestampDesc(
                    projectId, level, pageable);
        } else if (agentRole != null) {
            result = logRepository.findByProjectIdAndAgentRoleOrderByTimestampDesc(
                    projectId, agentRole, pageable);
        } else {
            result = logRepository.findByProjectIdOrderByTimestampDesc(projectId, pageable);
        }

        log.debug("[로그 조회] 완료 - projectId={}, 총 {}건", projectId, result.getTotalElements());
        return result;
    }
}
