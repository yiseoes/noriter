package com.noriter.exception;

import lombok.Getter;

@Getter
public class NoriterException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    public NoriterException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public NoriterException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
