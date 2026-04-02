package com.noriter.service;

import com.noriter.domain.Artifact;
import com.noriter.domain.Project;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.ArtifactType;
import com.noriter.domain.enums.AuditEventType;
import com.noriter.repository.ArtifactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class ArtifactService {

    private final ArtifactRepository artifactRepository;
    private final AuditService auditService;

    @Transactional
    public Artifact saveArtifact(Project project, ArtifactType type, AgentRole agentRole, String filePath) {
        log.info("[산출물 저장] 시작 - projectId={}, type={}, agent={}, path={}",
                project.getId(), type, agentRole, filePath);

        int nextVersion = getNextVersion(project.getId(), type);
        Artifact artifact = Artifact.create(project, type, agentRole, nextVersion, filePath);
        artifactRepository.save(artifact);

        log.info("[산출물 저장] 완료 - id={}, type={}, version={}",
                artifact.getId(), type, nextVersion);

        auditService.log(AuditEventType.ARTIFACT_CREATED, project.getId(),
                String.format("%s 산출물 생성 (v%d, %s)", type, nextVersion, agentRole), null);

        return artifact;
    }

    @Transactional(readOnly = true)
    public List<Artifact> getArtifacts(String projectId) {
        log.debug("[산출물 목록 조회] projectId={}", projectId);
        List<Artifact> artifacts = artifactRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        log.debug("[산출물 목록 조회] projectId={}, {}건 조회됨", projectId, artifacts.size());
        return artifacts;
    }

    @Transactional(readOnly = true)
    public Optional<Artifact> getLatestArtifact(String projectId, ArtifactType type) {
        log.debug("[최신 산출물 조회] projectId={}, type={}", projectId, type);
        return artifactRepository.findTopByProjectIdAndTypeOrderByVersionDesc(projectId, type);
    }

    private int getNextVersion(String projectId, ArtifactType type) {
        return artifactRepository.findTopByProjectIdAndTypeOrderByVersionDesc(projectId, type)
                .map(a -> a.getVersion() + 1)
                .orElse(1);
    }
}
