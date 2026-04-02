package com.noriter.controller.dto.response;

import com.noriter.domain.Project;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API-PRJ-002 목록 항목 / API-PRJ-001 생성 응답
 * 참조: 05_API §4 API-PRJ-001, API-PRJ-002 응답
 */
@Getter
public class ProjectResponse {

    private final String id;
    private final String name;
    private final String requirement;
    private final String genre;
    private final String status;
    private final String currentStage;
    private final int progress;
    private final LocalDateTime createdAt;
    private final LocalDateTime completedAt;
    private final boolean demo;

    public ProjectResponse(Project project) {
        this.id = project.getId();
        this.name = project.getName();
        this.requirement = project.getRequirement();
        this.genre = project.getGenre() != null ? project.getGenre().name() : null;
        this.status = project.getStatus().name();
        this.currentStage = project.getCurrentStage() != null ? project.getCurrentStage().name() : null;
        this.progress = project.getProgress();
        this.createdAt = project.getCreatedAt();
        this.completedAt = project.getCompletedAt();
        this.demo = project.isDemo();
    }
}
