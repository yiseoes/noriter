package com.noriter.controller.dto.response;

import com.noriter.domain.Project;
import com.noriter.domain.enums.AgentRole;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * API-PRJ-003: 프로젝트 상세 조회 응답
 * 참조: 05_API §4 API-PRJ-003 응답 — stages[], artifacts[], tokenUsage 포함
 */
@Getter
public class ProjectDetailResponse {

    private final String id;
    private final String name;
    private final String requirement;
    private final String genre;
    private final String status;
    private final String currentStage;
    private final int progress;
    private final boolean demo;
    private final LocalDateTime createdAt;
    private final LocalDateTime completedAt;
    private final List<StageResponse> stages;
    private final List<ArtifactResponse> artifacts;
    private final int debugAttempts;
    private final int maxDebugAttempts;
    private final TokenUsageResponse tokenUsage;

    public ProjectDetailResponse(Project project, List<StageResponse> stages,
                                  List<ArtifactResponse> artifacts,
                                  long totalTokens, Map<AgentRole, Long> tokensByAgent) {
        this.id = project.getId();
        this.name = project.getName();
        this.requirement = project.getRequirement();
        this.genre = project.getGenre() != null ? project.getGenre().name() : null;
        this.status = project.getStatus().name();
        this.currentStage = project.getCurrentStage() != null ? project.getCurrentStage().name() : null;
        this.progress = project.getProgress();
        this.demo = project.isDemo();
        this.createdAt = project.getCreatedAt();
        this.completedAt = project.getCompletedAt();
        this.stages = stages;
        this.artifacts = artifacts;
        this.debugAttempts = project.getDebugAttempts();
        this.maxDebugAttempts = project.getMaxDebugAttempts();
        this.tokenUsage = new TokenUsageResponse(totalTokens, tokensByAgent);
    }
}
