package com.noriter.service;

import com.noriter.domain.AuditLog;
import com.noriter.domain.enums.AuditEventType;
import com.noriter.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @InjectMocks
    private AuditService auditService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("감사 로그를 정상적으로 기록한다")
    void log_savesAuditLog() {
        auditService.log(AuditEventType.PROJECT_CREATED, "prj_test1234",
                "프로젝트 '테스트' 생성", "{\"genre\":\"ACTION\"}");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(AuditEventType.PROJECT_CREATED);
        assertThat(saved.getProjectId()).isEqualTo("prj_test1234");
        assertThat(saved.getDescription()).contains("테스트");
        assertThat(saved.getDetail()).contains("ACTION");
    }

    @Test
    @DisplayName("프로젝트 없는 시스템 이벤트도 기록할 수 있다")
    void log_withoutProject_savesAuditLog() {
        auditService.log(AuditEventType.SETTING_CHANGED, null,
                "API 키 변경", null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getProjectId()).isNull();
        assertThat(saved.getDetail()).isNull();
    }
}
