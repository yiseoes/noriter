package com.noriter.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorTest {

    @Test
    @DisplayName("프로젝트 ID는 prj_ 접두사를 가진다")
    void generateProjectId_hasPrefix() {
        String id = IdGenerator.generateProjectId();
        assertThat(id).startsWith("prj_");
        assertThat(id).hasSize(12); // prj_ + 8자
    }

    @Test
    @DisplayName("스테이지 ID는 stg_ 접두사를 가진다")
    void generateStageId_hasPrefix() {
        assertThat(IdGenerator.generateStageId()).startsWith("stg_");
    }

    @Test
    @DisplayName("산출물 ID는 art_ 접두사를 가진다")
    void generateArtifactId_hasPrefix() {
        assertThat(IdGenerator.generateArtifactId()).startsWith("art_");
    }

    @Test
    @DisplayName("로그 ID는 log_ 접두사를 가진다")
    void generateLogId_hasPrefix() {
        assertThat(IdGenerator.generateLogId()).startsWith("log_");
    }

    @Test
    @DisplayName("감사 ID는 audit_ 접두사를 가진다")
    void generateAuditId_hasPrefix() {
        assertThat(IdGenerator.generateAuditId()).startsWith("audit_");
    }

    @Test
    @DisplayName("메시지 ID는 msg_ 접두사를 가진다")
    void generateMessageId_hasPrefix() {
        assertThat(IdGenerator.generateMessageId()).startsWith("msg_");
    }

    @Test
    @DisplayName("생성된 ID는 매번 고유하다")
    void generatedIds_areUnique() {
        String id1 = IdGenerator.generateProjectId();
        String id2 = IdGenerator.generateProjectId();
        assertThat(id1).isNotEqualTo(id2);
    }
}
