package com.noriter.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Validation
    REQUIREMENT_TOO_SHORT("NT-ERR-V001", 400, "요구사항은 최소 10자 이상 입력해야 합니다."),
    NAME_TOO_LONG("NT-ERR-V002", 400, "프로젝트명은 100자를 초과할 수 없습니다."),
    INVALID_GENRE("NT-ERR-V003", 400, "유효하지 않은 장르입니다."),
    FEEDBACK_TOO_SHORT("NT-ERR-V004", 400, "수정 요청은 최소 5자 이상 입력해야 합니다."),

    // Project
    PROJECT_NOT_FOUND("NT-ERR-P001", 404, "프로젝트를 찾을 수 없습니다."),
    RETRY_NOT_ALLOWED("NT-ERR-P002", 409, "재시도는 실패(FAILED) 상태의 프로젝트만 가능합니다."),
    DELETE_NOT_ALLOWED("NT-ERR-P003", 409, "진행 중인 프로젝트는 삭제할 수 없습니다. 먼저 중단해주세요."),
    FEEDBACK_NOT_ALLOWED("NT-ERR-P004", 409, "수정 요청은 완료(COMPLETED) 상태의 프로젝트만 가능합니다."),
    CANCEL_NOT_ALLOWED("NT-ERR-P005", 409, "중단은 진행 중(IN_PROGRESS/REVISION) 상태의 프로젝트만 가능합니다."),

    // Agent / API
    CLAUDE_RATE_LIMIT("NT-ERR-A001", 500, "Claude API 호출 빈도 제한에 도달했습니다."),
    CLAUDE_AUTH_FAILED("NT-ERR-A002", 500, "Claude API 인증에 실패했습니다."),
    CLAUDE_TIMEOUT("NT-ERR-A003", 500, "Claude API 응답 시간이 초과되었습니다."),
    AGENT_EXECUTION_FAILED("NT-ERR-A004", 500, "에이전트 실행에 실패했습니다."),
    PIPELINE_TIMEOUT("NT-ERR-A005", 500, "파이프라인 실행 시간이 초과되었습니다."),

    // Game
    GAME_NOT_FOUND("NT-ERR-G001", 404, "게임 파일이 아직 생성되지 않았습니다."),

    // System
    API_KEY_NOT_CONFIGURED("NT-ERR-S001", 503, "API 키가 설정되지 않았습니다. 설정 페이지에서 API 키를 등록해주세요."),
    INTERNAL_SERVER_ERROR("NT-ERR-S002", 500, "내부 서버 오류가 발생했습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
