package com.noriter.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API-PRJ-001: 게임 생성 요청
 * 참조: 05_API §4 API-PRJ-001
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @Size(max = 100, message = "프로젝트명은 100자를 초과할 수 없습니다")  // NT-ERR-V002
    private String name;

    @NotBlank(message = "요구사항은 필수입니다")
    @Size(min = 10, message = "요구사항은 최소 10자 이상 입력해야 합니다")  // NT-ERR-V001
    private String requirement;

    private String genre;  // PUZZLE, ACTION, ARCADE, SHOOTING, STRATEGY, OTHER (nullable)

    private boolean demo;  // true면 더미 에이전트 사용, API 키 불필요
}
