package com.noriter.domain;

import com.noriter.domain.enums.StageStatus;
import com.noriter.domain.enums.StageType;
import com.noriter.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stage")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stage {

    @Id
    @Column(length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StageType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StageStatus status;

    @Column(name = "agent_role", nullable = false, length = 30)
    private String agentRole;

    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "artifact_id", length = 20)
    private String artifactId;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public static Stage create(Project project, StageType type, String agentRole, int stageOrder) {
        Stage stage = new Stage();
        stage.id = IdGenerator.generateStageId();
        stage.project = project;
        stage.type = type;
        stage.status = StageStatus.PENDING;
        stage.agentRole = agentRole;
        stage.stageOrder = stageOrder;
        stage.retryCount = 0;
        return stage;
    }

    public void start() {
        this.status = StageStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void complete(String artifactId) {
        this.status = StageStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.artifactId = artifactId;
    }

    public void fail(String errorMessage) {
        this.status = StageStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void retry() {
        this.status = StageStatus.RETRYING;
        this.retryCount++;
        this.errorMessage = null;
    }

    public void skip() {
        this.status = StageStatus.SKIPPED;
    }
}
