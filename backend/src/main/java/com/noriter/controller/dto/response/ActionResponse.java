package com.noriter.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API-PRJ-004~007 공통 액션 응답
 * 참조: 05_API §4 API-PRJ-004, 006, 007 응답
 */
@Getter
@AllArgsConstructor
public class ActionResponse {

    private final String id;
    private final String status;
    private final String message;
}
