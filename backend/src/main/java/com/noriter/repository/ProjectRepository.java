package com.noriter.repository;

import com.noriter.domain.Project;
import com.noriter.domain.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, String> {

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    Page<Project> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByGuestIdAndDemoFalse(String guestId);

    Page<Project> findByGuestIdOrderByCreatedAtDesc(String guestId, Pageable pageable);

    Page<Project> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndDemoFalse(Long userId);
}
