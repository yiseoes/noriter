package com.noriter.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API-SET-002, API-SET-003: API 키 설정/검증
 * 참조: 05_API §4 API-SET-002, API-SET-003
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyRequest {

    @NotBlank(message = "API 키는 필수입니다")
    private String apiKey;
}
