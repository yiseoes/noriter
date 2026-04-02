package com.noriter.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API-PRJ-004: 프로젝트 재시도
 * 참조: 05_API §4 API-PRJ-004
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RetryProjectRequest {

    private String fromStage;  // nullable — 미입력 시 처음부터 재실행
}
