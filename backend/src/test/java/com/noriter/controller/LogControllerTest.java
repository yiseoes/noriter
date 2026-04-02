package com.noriter.controller;

import com.noriter.domain.AuditLog;
import com.noriter.domain.LogEntry;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.AuditEventType;
import com.noriter.domain.enums.LogLevel;
import com.noriter.domain.enums.StageType;
import com.noriter.repository.AgentMessageRepository;
import com.noriter.service.AuditService;
import com.noriter.service.LogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogController.class)
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogService logService;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private AgentMessageRepository agentMessageRepository;

    @Test
    @DisplayName("API-LOG-001: 프로젝트 로그를 조회한다")
    void getProjectLogs_success() throws Exception {
        LogEntry log = LogEntry.create("prj_test", LogLevel.INFO, AgentRole.PLANNING,
                StageType.PLANNING, "기획팀이 작업을 시작합니다.");
        when(logService.getLogs(eq("prj_test"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log)));

        mockMvc.perform(get("/api/projects/prj_test/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].level").value("INFO"))
                .andExpect(jsonPath("$.content[0].agentRole").value("PLANNING"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("API-LOG-001: 레벨 필터로 로그를 조회한다")
    void getProjectLogs_withLevelFilter() throws Exception {
        when(logService.getLogs(eq("prj_test"), eq(LogLevel.ERROR), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/projects/prj_test/logs?level=ERROR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("API-LOG-002: 에이전트 대화 로그를 조회한다")
    void getAgentMessages_success() throws Exception {
        when(agentMessageRepository.findByProjectIdOrderByCreatedAtAsc("prj_test"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/projects/prj_test/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray());
    }

    @Test
    @DisplayName("API-LOG-003: 감사 로그를 조회한다")
    void getAuditLogs_success() throws Exception {
        AuditLog auditLog = AuditLog.create(AuditEventType.PROJECT_CREATED,
                "prj_test", "프로젝트 생성", null);
        when(auditService.getAuditLogs(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(auditLog)));

        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventType").value("PROJECT_CREATED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("API-LOG-004: 에러 코드 상세를 조회한다")
    void getErrorCodeDetail_success() throws Exception {
        mockMvc.perform(get("/api/error-codes/NT-ERR-A001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NT-ERR-A001"))
                .andExpect(jsonPath("$.name").value("Claude API Rate Limit"))
                .andExpect(jsonPath("$.category").value("API"));
    }

    @Test
    @DisplayName("API-LOG-004: 없는 에러 코드는 404를 반환한다")
    void getErrorCodeDetail_notFound() throws Exception {
        mockMvc.perform(get("/api/error-codes/NT-ERR-XXXX"))
                .andExpect(status().isNotFound());
    }
}
