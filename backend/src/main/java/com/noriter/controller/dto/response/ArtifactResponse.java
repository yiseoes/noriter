package com.noriter.controller.dto.response;

import com.noriter.domain.Artifact;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API-PRJ-003 상세 응답 내 artifacts[] 항목
 * 참조: 05_API §4 API-PRJ-003 artifacts 배열
 */
@Getter
public class ArtifactResponse {

    private final String id;
    private final String type;
    private final String agentRole;
    private final int version;
    private final LocalDateTime createdAt;

    public ArtifactResponse(Artifact artifact) {
        this.id = artifact.getId();
        this.type = artifact.getType().name();
        this.agentRole = artifact.getAgentRole().name();
        this.version = artifact.getVersion();
        this.createdAt = artifact.getCreatedAt();
    }
}
