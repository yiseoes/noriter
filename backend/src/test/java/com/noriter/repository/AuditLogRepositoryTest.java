package com.noriter.repository;

import com.noriter.domain.AuditLog;
import com.noriter.domain.enums.AuditEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("감사 로그를 저장하고 조회할 수 있다")
    void save_and_findAll() {
        AuditLog log = AuditLog.create(
                AuditEventType.PROJECT_CREATED,
                "prj_test1234",
                "프로젝트 '테스트 게임' 생성",
                "{\"projectName\":\"테스트 게임\",\"genre\":\"ACTION\"}"
        );
        auditLogRepository.save(log);

        Page<AuditLog> result = auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, 50));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getEventType()).isEqualTo(AuditEventType.PROJECT_CREATED);
        assertThat(result.getContent().get(0).getProjectId()).isEqualTo("prj_test1234");
    }

    @Test
    @DisplayName("이벤트 타입별로 감사 로그를 필터링할 수 있다")
    void findByEventType_filtersCorrectly() {
        AuditLog log1 = AuditLog.create(AuditEventType.PROJECT_CREATED, "prj_1", "프로젝트 생성", null);
        AuditLog log2 = AuditLog.create(AuditEventType.SETTING_CHANGED, null, "API 키 변경", null);
        auditLogRepository.save(log1);
        auditLogRepository.save(log2);

        Page<AuditLog> settings = auditLogRepository.findByEventTypeOrderByTimestampDesc(
                AuditEventType.SETTING_CHANGED, PageRequest.of(0, 50));

        assertThat(settings.getTotalElements()).isEqualTo(1);
        assertThat(settings.getContent().get(0).getProjectId()).isNull();
    }

    @Test
    @DisplayName("프로젝트별로 감사 로그를 조회할 수 있다")
    void findByProjectId_filtersCorrectly() {
        AuditLog log1 = AuditLog.create(AuditEventType.PROJECT_CREATED, "prj_aaa", "프로젝트 A 생성", null);
        AuditLog log2 = AuditLog.create(AuditEventType.PROJECT_CREATED, "prj_bbb", "프로젝트 B 생성", null);
        auditLogRepository.save(log1);
        auditLogRepository.save(log2);

        Page<AuditLog> result = auditLogRepository.findByProjectIdOrderByTimestampDesc(
                "prj_aaa", PageRequest.of(0, 50));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getDescription()).contains("A");
    }
}
