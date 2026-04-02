package com.noriter.repository;

import com.noriter.domain.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StageRepository extends JpaRepository<Stage, String> {

    List<Stage> findByProjectIdOrderByStageOrderAsc(String projectId);
}
