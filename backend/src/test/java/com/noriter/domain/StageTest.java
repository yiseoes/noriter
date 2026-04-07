package com.noriter.domain;

import com.noriter.domain.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StageTest {

    private Project createProject() {
        return Project.create("테스트", "요구사항 10자 이상입니다", Genre.ACTION, 3, false, null, null);
    }

    @Test
    @DisplayName("스테이지 생성 시 기본값이 올바르게 설정된다")
    void create_setsDefaultValues() {
        Project project = createProject();
        Stage stage = Stage.create(project, StageType.PLANNING, "PLANNING", 1);

        assertThat(stage.getId()).startsWith("stg_");
        assertThat(stage.getProject()).isEqualTo(project);
        assertThat(stage.getType()).isEqualTo(StageType.PLANNING);
        assertThat(stage.getStatus()).isEqualTo(StageStatus.PENDING);
        assertThat(stage.getAgentRole()).isEqualTo("PLANNING");
        assertThat(stage.getStageOrder()).isEqualTo(1);
        assertThat(stage.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("스테이지를 시작하면 IN_PROGRESS 상태가 된다")
    void start_changesStatusToInProgress() {
        Stage stage = Stage.create(createProject(), StageType.PLANNING, "PLANNING", 1);

        stage.start();

        assertThat(stage.getStatus()).isEqualTo(StageStatus.IN_PROGRESS);
        assertThat(stage.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("스테이지 완료 시 산출물 ID가 설정된다")
    void complete_setsArtifactId() {
        Stage stage = Stage.create(createProject(), StageType.PLANNING, "PLANNING", 1);
        stage.start();

        stage.complete("art_12345678");

        assertThat(stage.getStatus()).isEqualTo(StageStatus.COMPLETED);
        assertThat(stage.getCompletedAt()).isNotNull();
        assertThat(stage.getArtifactId()).isEqualTo("art_12345678");
    }

    @Test
    @DisplayName("스테이지 실패 시 에러 메시지가 저장된다")
    void fail_setsErrorMessage() {
        Stage stage = Stage.create(createProject(), StageType.QA, "QA", 5);
        stage.start();

        stage.fail("Claude API 호출 실패");

        assertThat(stage.getStatus()).isEqualTo(StageStatus.FAILED);
        assertThat(stage.getErrorMessage()).isEqualTo("Claude API 호출 실패");
    }

    @Test
    @DisplayName("스테이지 재시도 시 카운트가 증가한다")
    void retry_incrementsCount() {
        Stage stage = Stage.create(createProject(), StageType.IMPLEMENTATION, "FRONTEND,BACKEND", 4);

        stage.retry();

        assertThat(stage.getStatus()).isEqualTo(StageStatus.RETRYING);
        assertThat(stage.getRetryCount()).isEqualTo(1);
        assertThat(stage.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("스테이지를 건너뛸 수 있다")
    void skip_changesStatusToSkipped() {
        Stage stage = Stage.create(createProject(), StageType.DEBUG, "BACKEND", 6);

        stage.skip();

        assertThat(stage.getStatus()).isEqualTo(StageStatus.SKIPPED);
    }
}
