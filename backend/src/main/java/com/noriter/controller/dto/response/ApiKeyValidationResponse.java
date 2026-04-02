package com.noriter.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API-SET-003: API 키 유효성 검증 응답
 * 참조: 05_API §4 API-SET-003 응답
 */
@Getter
@AllArgsConstructor
public class ApiKeyValidationResponse {

    private final boolean valid;
    private final String model;
    private final String message;
}
