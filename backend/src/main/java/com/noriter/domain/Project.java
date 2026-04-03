package com.noriter.domain;

import com.noriter.domain.enums.Genre;
import com.noriter.domain.enums.ProjectStatus;
import com.noriter.domain.enums.StageType;
import com.noriter.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requirement;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Genre genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", length = 20)
    private StageType currentStage;

    @Column(nullable = false)
    private Integer progress;

    @Column(name = "debug_attempts", nullable = false)
    private Integer debugAttempts;

    @Column(name = "max_debug_attempts", nullable = false)
    private Integer maxDebugAttempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean demo;

    @Column(name = "guest_id", length = 30)
    private String guestId;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stageOrder ASC")
    private List<Stage> stages = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Artifact> artifacts = new ArrayList<>();

    public static Project create(String name, String requirement, Genre genre, int maxDebugAttempts, boolean demo, String guestId) {
        Project project = new Project();
        project.id = IdGenerator.generateProjectId();
        project.name = name;
        project.requirement = requirement;
        project.genre = genre;
        project.status = ProjectStatus.CREATED;
        project.progress = 0;
        project.debugAttempts = 0;
        project.maxDebugAttempts = maxDebugAttempts;
        project.demo = demo;
        project.guestId = guestId;
        project.createdAt = LocalDateTime.now();
        project.updatedAt = LocalDateTime.now();
        return project;
    }

    public void updateStatus(ProjectStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        if (newStatus == ProjectStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void updateProgress(int progress, StageType currentStage) {
        this.progress = progress;
        this.currentStage = currentStage;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementDebugAttempts() {
        this.debugAttempts++;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canRetry() {
        return this.status == ProjectStatus.FAILED;
    }

    public boolean canCancel() {
        return this.status == ProjectStatus.IN_PROGRESS || this.status == ProjectStatus.REVISION;
    }

    public boolean canDelete() {
        return this.status != ProjectStatus.IN_PROGRESS && this.status != ProjectStatus.REVISION;
    }

    public boolean canFeedback() {
        return this.status == ProjectStatus.COMPLETED;
    }
}
