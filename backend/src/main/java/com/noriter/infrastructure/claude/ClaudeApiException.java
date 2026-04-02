package com.noriter.infrastructure.claude;

import lombok.Getter;

/**
 * Claude API 호출 에러 래핑
 * 참조: 05_API §8 에러 코드 NT-ERR-A001~A003
 */
@Getter
public class ClaudeApiException extends RuntimeException {

    private final String errorCode;

    public ClaudeApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ClaudeApiException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public static ClaudeApiException rateLimited() {
        return new ClaudeApiException("NT-ERR-A001", "Claude API 호출 빈도 제한에 도달했습니다.");
    }

    public static ClaudeApiException authFailed() {
        return new ClaudeApiException("NT-ERR-A002", "Claude API 인증에 실패했습니다.");
    }

    public static ClaudeApiException timeout() {
        return new ClaudeApiException("NT-ERR-A003", "Claude API 응답 시간이 초과되었습니다.");
    }
}
