package com.noriter.service;

import com.noriter.domain.Project;
import com.noriter.domain.enums.Genre;
import com.noriter.domain.enums.ProjectStatus;
import com.noriter.domain.enums.StageType;
import com.noriter.exception.ErrorCode;
import com.noriter.exception.NoriterException;
import com.noriter.repository.ProjectRepository;
import com.noriter.repository.StageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @InjectMocks
    private ProjectService projectService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private StageRepository stageRepository;

    @Mock
    private AuditService auditService;

    @Test
    @DisplayName("프로젝트를 생성하면 CREATED 상태와 6개 스테이지가 생성된다")
    void createProject_success() {
        ReflectionTestUtils.setField(projectService, "maxDebugAttempts", 3);
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArgument(0));
        when(stageRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        Project project = projectService.createProject("테스트 게임", "뱀파이어 서바이벌 미니게임을 만들어줘", Genre.ACTION, false, null, null);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CREATED);
        assertThat(project.getName()).isEqualTo("테스트 게임");
        assertThat(project.getStages()).hasSize(6);
        verify(projectRepository).save(any(Project.class));
        verify(auditService).log(any(), any(), any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트 조회 시 예외가 발생한다")
    void getProject_notFound_throwsException() {
        when(projectRepository.findById("prj_invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject("prj_invalid"))
                .isInstanceOf(NoriterException.class)
                .satisfies(e -> assertThat(((NoriterException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PROJECT_NOT_FOUND));
    }

    @Test
    @DisplayName("프로젝트 목록을 상태별로 필터링할 수 있다")
    void getProjects_withStatusFilter() {
        Project project = Project.create("게임", "요구사항 10자 이상입니다", Genre.ACTION, 3, false, null, null);
        Page<Project> page = new PageImpl<>(List.of(project));
        when(projectRepository.findByStatus(eq(ProjectStatus.CREATED), any())).thenReturn(page);

        Page<Project> result = projectService.getProjects(ProjectStatus.CREATED, PageRequest.of(0, 20), null, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("FAILED 상태의 프로젝트를 재시도할 수 있다")
    void retryProject_whenFailed_success() {
        Project project = Project.create("게임", "요구사항 10자 이상입니다", Genre.ACTION, 3, false, null, null);
        project.updateStatus(ProjectStatus.FAILED);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        Project result = projectService.retryProject(project.getId(), StageType.IMPLEMENTATION, null, null);

        assertThat(result.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("FAILED가 아닌 프로젝트를 재시도하면 예외가 발생한다")
    void retryProject_whenNotFailed_throwsException() {
        Project project = Project.create("게임", "요구사항 10자 이상입니다", Genre.ACTION, 3, false, null, null);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.retryProject(project.getId(), null, null, null))
                .isInstanceOf(NoriterException.class)
                .satisfies(e -> assertThat(((NoriterException) e).getErrorCode())
                        .isEqualTo(ErrorCode.RETRY_NOT_ALLOWED));
    }

    @Test
    @DisplayName("진행 중인 프로젝트는 삭제할 수 없다")
    void deleteProject_whenInProgress_throwsException() {
        Project project = Project.create("게임", "요구사항 10자 이상입니다", Genre.ACTION, 3, false, null, null);
        project.updateStatus(ProjectStatus.IN_PROGRESS);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.deleteProject(project.getId(), null, null))
                .isInstanceOf(NoriterException.class)
                .satisfies(e -> assertThat(((NoriterException) e).getErrorCode())
                        .isEqualTo(ErrorCode.DELETE_NOT_ALLOWED));
    }

    @Test
    @DisplayName("COMPLETED가 아닌 프로젝트에 피드백을 보내면 예외가 발생한다")
    void requestFeedback_whenNotCompleted_throwsException() {
        Project project = Project.create("게임", "요구사항 10자 이상입니다", Genre.ACTION, 3, false, null, null);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.requestFeedback(project.getId(), "속도가 너무 빨라요", null, null))
                .isInstanceOf(NoriterException.class)
                .satisfies(e -> assertThat(((NoriterException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FEEDBACK_NOT_ALLOWED));
    }

    @Test
    @DisplayName("IN_PROGRESS 상태의 프로젝트를 중단할 수 있다")
    void cancelProject_whenInProgress_success() {
        Project project = Project.create("게임", "요구사항 10자 이상입니다", Genre.ACTION, 3, false, null, null);
        project.updateStatus(ProjectStatus.IN_PROGRESS);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        Project result = projectService.cancelProject(project.getId(), null, null);

        assertThat(result.getStatus()).isEqualTo(ProjectStatus.CANCELLED);
    }
}
