package com.noriter.controller.dto.response;

import com.noriter.domain.Stage;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API-PRJ-003 상세 응답 내 stages[] 항목
 * 참조: 05_API §4 API-PRJ-003 stages 배열
 */
@Getter
public class StageResponse {

    private final String type;
    private final String status;
    private final String agentRole;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final String artifactId;

    public StageResponse(Stage stage) {
        this.type = stage.getType().name();
        this.status = stage.getStatus().name();
        this.agentRole = stage.getAgentRole();
        this.startedAt = stage.getStartedAt();
        this.completedAt = stage.getCompletedAt();
        this.artifactId = stage.getArtifactId();
    }
}
