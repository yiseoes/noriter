package com.noriter.domain;

import com.noriter.domain.enums.Genre;
import com.noriter.domain.enums.ProjectStatus;
import com.noriter.domain.enums.StageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectTest {

    @Test
    @DisplayName("프로젝트 생성 시 기본값이 올바르게 설정된다")
    void create_setsDefaultValues() {
        Project project = Project.create("테스트 게임", "테스트 요구사항입니다", Genre.ACTION, 3, false, null);

        assertThat(project.getId()).startsWith("prj_");
        assertThat(project.getName()).isEqualTo("테스트 게임");
        assertThat(project.getRequirement()).isEqualTo("테스트 요구사항입니다");
        assertThat(project.getGenre()).isEqualTo(Genre.ACTION);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CREATED);
        assertThat(project.getProgress()).isZero();
        assertThat(project.getDebugAttempts()).isZero();
        assertThat(project.getMaxDebugAttempts()).isEqualTo(3);
        assertThat(project.getCreatedAt()).isNotNull();
        assertThat(project.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("상태를 COMPLETED로 변경하면 completedAt이 설정된다")
    void updateStatus_toCompleted_setsCompletedAt() {
        Project project = Project.create("게임", "요구사항 10자 이상", Genre.PUZZLE, 3, false, null);

        project.updateStatus(ProjectStatus.COMPLETED);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(project.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("진행률과 현재 스테이지를 업데이트한다")
    void updateProgress_updatesValues() {
        Project project = Project.create("게임", "요구사항 10자 이상", Genre.ACTION, 3, false, null);

        project.updateProgress(57, StageType.IMPLEMENTATION);

        assertThat(project.getProgress()).isEqualTo(57);
        assertThat(project.getCurrentStage()).isEqualTo(StageType.IMPLEMENTATION);
    }

    @Test
    @DisplayName("디버깅 시도 횟수를 증가시킨다")
    void incrementDebugAttempts_incrementsCount() {
        Project project = Project.create("게임", "요구사항 10자 이상", Genre.ACTION, 3, false, null);

        project.incrementDebugAttempts();
        project.incrementDebugAttempts();

        assertThat(project.getDebugAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("FAILED 상태에서만 재시도 가능하다")
    void canRetry_onlyWhenFailed() {
        Project project = Project.create("게임", "요구사항 10자 이상", Genre.ACTION, 3, false, null);

        assertThat(project.canRetry()).isFalse(); // CREATED

        project.updateStatus(ProjectStatus.FAILED);
        assertThat(project.canRetry()).isTrue();

        project.updateStatus(ProjectStatus.COMPLETED);
        assertThat(project.canRetry()).isFalse();
    }

    @Test
    @DisplayName("IN_PROGRESS 또는 REVISION 상태에서만 취소 가능하다")
    void canCancel_onlyWhenInProgressOrRevision() {
        Project project = Project.create("게임", "요구사항 10자 이상", Genre.ACTION, 3, false, null);

        assertThat(project.canCancel()).isFalse(); // CREATED

        project.updateStatus(ProjectStatus.IN_PROGRESS);
        assertThat(project.canCancel()).isTrue();

        project.updateStatus(ProjectStatus.REVISION);
        assertThat(project.canCancel()).isTrue();
    }

    @Test
    @DisplayName("IN_PROGRESS/REVISION 상태에서는 삭제 불가하다")
    void canDelete_notWhenInProgress() {
        Project project = Project.create("게임", "요구사항 10자 이상", Genre.ACTION, 3, false, null);

        assertThat(project.canDelete()).isTrue(); // CREATED

        project.updateStatus(ProjectStatus.IN_PROGRESS);
        assertThat(project.canDelete()).isFalse();

        project.updateStatus(ProjectStatus.FAILED);
        assertThat(project.canDelete()).isTrue();
    }

    @Test
    @DisplayName("COMPLETED 상태에서만 피드백 가능하다")
    void canFeedback_onlyWhenCompleted() {
        Project project = Project.create("게임", "요구사항 10자 이상", Genre.ACTION, 3, false, null);

        assertThat(project.canFeedback()).isFalse();

        project.updateStatus(ProjectStatus.COMPLETED);
        assertThat(project.canFeedback()).isTrue();
    }
}
