package com.noriter.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API-SET-001: API 키 조회 응답 (마스킹)
 * 참조: 05_API §4 API-SET-001 응답
 */
@Getter
@AllArgsConstructor
public class ApiKeyResponse {

    private final boolean configured;
    private final String maskedKey;
    private final LocalDateTime updatedAt;
}
