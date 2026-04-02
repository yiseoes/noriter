package com.noriter.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API-SYS-001: 시스템 헬스체크 응답
 * 참조: 05_API §4 API-SYS-001 응답
 */
@Getter
@AllArgsConstructor
public class HealthResponse {

    private final String status;
    private final Map<String, Object> components;
    private final LocalDateTime timestamp;
}
