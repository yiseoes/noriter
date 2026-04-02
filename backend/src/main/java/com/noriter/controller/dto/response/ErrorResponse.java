package com.noriter.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final Error error;

    public ErrorResponse(String code, String message, String detail) {
        this.error = new Error(code, message, detail, LocalDateTime.now());
    }

    @Getter
    @AllArgsConstructor
    public static class Error {
        private final String code;
        private final String message;
        private final String detail;
        private final LocalDateTime timestamp;
    }
}
