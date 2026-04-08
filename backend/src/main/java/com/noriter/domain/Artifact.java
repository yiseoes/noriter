package com.noriter.domain;

import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.ArtifactType;
import com.noriter.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "artifact")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artifact {

    @Id
    @Column(length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArtifactType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_role", nullable = false, length = 30)
    private AgentRole agentRole;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Artifact create(Project project, ArtifactType type, AgentRole agentRole, int version, String filePath) {
        Artifact artifact = new Artifact();
        artifact.id = IdGenerator.generateArtifactId();
        artifact.project = project;
        artifact.type = type;
        artifact.agentRole = agentRole;
        artifact.version = version;
        artifact.filePath = filePath;
        artifact.createdAt = LocalDateTime.now();
        return artifact;
    }
}
