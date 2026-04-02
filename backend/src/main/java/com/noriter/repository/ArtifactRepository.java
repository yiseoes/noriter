package com.noriter.repository;

import com.noriter.domain.Artifact;
import com.noriter.domain.enums.ArtifactType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtifactRepository extends JpaRepository<Artifact, String> {

    List<Artifact> findByProjectIdOrderByCreatedAtAsc(String projectId);

    Optional<Artifact> findTopByProjectIdAndTypeOrderByVersionDesc(String projectId, ArtifactType type);
}
