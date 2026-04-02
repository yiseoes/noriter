package com.noriter.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API-LOG-004: 에러 코드 상세 응답
 * 참조: 05_API §4 API-LOG-004 응답
 */
@Getter
@AllArgsConstructor
public class ErrorCodeResponse {

    private final String code;
    private final String name;
    private final String description;
    private final String cause;
    private final String resolution;
    private final String category;
    private final String severity;
}
