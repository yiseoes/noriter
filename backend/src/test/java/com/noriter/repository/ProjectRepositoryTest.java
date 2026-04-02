package com.noriter.repository;

import com.noriter.domain.Project;
import com.noriter.domain.enums.Genre;
import com.noriter.domain.enums.ProjectStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    @DisplayName("프로젝트를 저장하고 조회할 수 있다")
    void save_and_findById() {
        Project project = Project.create("테스트 게임", "뱀파이어 서바이벌 류 미니게임을 만들어줘", Genre.ACTION, 3, false);
        projectRepository.save(project);

        Optional<Project> found = projectRepository.findById(project.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("테스트 게임");
        assertThat(found.get().getGenre()).isEqualTo(Genre.ACTION);
        assertThat(found.get().getStatus()).isEqualTo(ProjectStatus.CREATED);
    }

    @Test
    @DisplayName("상태별로 프로젝트를 필터링할 수 있다")
    void findByStatus_filtersCorrectly() {
        Project p1 = Project.create("게임1", "요구사항 10자 이상입니다 1", Genre.ACTION, 3, false);
        Project p2 = Project.create("게임2", "요구사항 10자 이상입니다 2", Genre.PUZZLE, 3, false);
        p2.updateStatus(ProjectStatus.IN_PROGRESS);

        projectRepository.save(p1);
        projectRepository.save(p2);

        Page<Project> created = projectRepository.findByStatus(ProjectStatus.CREATED, PageRequest.of(0, 20));
        Page<Project> inProgress = projectRepository.findByStatus(ProjectStatus.IN_PROGRESS, PageRequest.of(0, 20));

        assertThat(created.getTotalElements()).isEqualTo(1);
        assertThat(inProgress.getTotalElements()).isEqualTo(1);
        assertThat(created.getContent().get(0).getName()).isEqualTo("게임1");
        assertThat(inProgress.getContent().get(0).getName()).isEqualTo("게임2");
    }

    @Test
    @DisplayName("프로젝트 삭제가 정상 동작한다")
    void delete_removesProject() {
        Project project = Project.create("삭제 테스트", "요구사항 10자 이상입니다", Genre.ARCADE, 3, false);
        projectRepository.save(project);

        projectRepository.deleteById(project.getId());

        assertThat(projectRepository.findById(project.getId())).isEmpty();
    }
}
