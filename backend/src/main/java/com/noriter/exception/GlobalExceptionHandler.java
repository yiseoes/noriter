package com.noriter.exception;

import com.noriter.controller.dto.response.ErrorResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoriterException.class)
    public ResponseEntity<ErrorResponse> handleNoriterException(NoriterException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[예외 처리] 비즈니스 예외 발생 - code={}, message={}, detail={}",
                errorCode.getCode(), errorCode.getMessage(), e.getDetail());

        ErrorResponse response = new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                e.getDetail()
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String field = e.getBindingResult().getFieldErrors().isEmpty()
                ? null : e.getBindingResult().getFieldErrors().get(0).getField();
        String message = e.getBindingResult().getFieldErrors().isEmpty()
                ? "유효성 검증에 실패했습니다."
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        log.warn("[예외 처리] 유효성 검증 실패 - field={}, message={}", field, message);

        ErrorResponse response = new ErrorResponse("NT-ERR-V000", message, field);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 클라이언트가 먼저 연결을 끊었을 때 발생 (탭 닫기, 페이지 이탈, SSE 종료).
     * 서버 오류가 아니므로 DEBUG로만 기록하고 응답하지 않음.
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientAbort(AsyncRequestNotUsableException e) {
        log.debug("[예외 처리] 클라이언트 연결 종료 (정상) - {}", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        log.error("[예외 처리] 예상치 못한 서버 오류 발생 - type={}, message={}",
                e.getClass().getSimpleName(), e.getMessage(), e);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse response = new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                e.getMessage()
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
}
