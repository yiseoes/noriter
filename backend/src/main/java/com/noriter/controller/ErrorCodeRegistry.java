package com.noriter.controller;

import com.noriter.controller.dto.response.ErrorCodeResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * 에러 코드 정적 레지스트리
 * 참조: 05_API §8 에러 코드 체계 (18종)
 */
public class ErrorCodeRegistry {

    private static final Map<String, ErrorCodeResponse> REGISTRY = new HashMap<>();

    static {
        register("NT-ERR-V001", "요구사항 길이 부족", "요구사항은 최소 10자 이상 입력해야 합니다.",
                "입력된 요구사항이 10자 미만인 경우", "요구사항을 더 상세하게 작성해주세요.", "VALIDATION", "WARN");
        register("NT-ERR-V002", "프로젝트명 길이 초과", "프로젝트명은 100자를 초과할 수 없습니다.",
                "프로젝트명이 100자를 초과한 경우", "프로젝트명을 줄여주세요.", "VALIDATION", "WARN");
        register("NT-ERR-V003", "유효하지 않은 장르", "유효하지 않은 장르입니다.",
                "허용되지 않은 장르 값이 입력된 경우", "PUZZLE, ACTION, ARCADE, SHOOTING, STRATEGY, OTHER 중 선택해주세요.", "VALIDATION", "WARN");
        register("NT-ERR-V004", "수정 요청 길이 부족", "수정 요청은 최소 5자 이상 입력해야 합니다.",
                "피드백이 5자 미만인 경우", "수정 요청을 더 구체적으로 작성해주세요.", "VALIDATION", "WARN");

        register("NT-ERR-P001", "프로젝트 없음", "프로젝트를 찾을 수 없습니다.",
                "존재하지 않는 프로젝트 ID로 조회한 경우", "프로젝트 ID를 확인해주세요.", "PROJECT", "ERROR");
        register("NT-ERR-P002", "재시도 불가", "재시도는 실패(FAILED) 상태의 프로젝트만 가능합니다.",
                "FAILED가 아닌 프로젝트에 재시도를 시도한 경우", "프로젝트 상태를 확인해주세요.", "PROJECT", "WARN");
        register("NT-ERR-P003", "삭제 불가", "진행 중인 프로젝트는 삭제할 수 없습니다.",
                "IN_PROGRESS 상태에서 삭제를 시도한 경우", "먼저 중단한 후 삭제해주세요.", "PROJECT", "WARN");
        register("NT-ERR-P004", "수정 요청 불가", "수정 요청은 완료(COMPLETED) 상태의 프로젝트만 가능합니다.",
                "COMPLETED가 아닌 프로젝트에 수정 요청한 경우", "프로젝트가 완료된 후 수정 요청해주세요.", "PROJECT", "WARN");
        register("NT-ERR-P005", "중단 불가", "중단은 진행 중(IN_PROGRESS/REVISION) 상태의 프로젝트만 가능합니다.",
                "진행 중이 아닌 프로젝트를 중단하려 한 경우", "프로젝트 상태를 확인해주세요.", "PROJECT", "WARN");

        register("NT-ERR-A001", "Claude API Rate Limit", "Claude API 호출 빈도 제한에 도달했습니다.",
                "단시간에 너무 많은 API 호출이 발생한 경우",
                "자동 재시도(지수 백오프)가 수행됩니다. 지속적으로 발생하면 잠시 후 다시 시도해주세요.", "API", "WARN");
        register("NT-ERR-A002", "Claude API 인증 실패", "Claude API 인증에 실패했습니다.",
                "잘못된 API 키가 설정된 경우", "설정 페이지에서 API 키를 확인해주세요.", "API", "ERROR");
        register("NT-ERR-A003", "Claude API 타임아웃", "Claude API 응답 시간이 초과되었습니다.",
                "API 서버 응답 지연", "자동 재시도가 수행됩니다. 지속되면 네트워크를 확인해주세요.", "API", "WARN");
        register("NT-ERR-A004", "에이전트 실행 실패", "에이전트 실행에 실패했습니다.",
                "에이전트 내부 오류 (JSON 파싱 실패 등)", "자동 재시도가 수행됩니다. 지속되면 재시도해주세요.", "API", "ERROR");
        register("NT-ERR-A005", "파이프라인 타임아웃", "파이프라인 실행 시간이 초과되었습니다.",
                "전체 파이프라인 30분 초과", "요구사항을 간소화하거나 재시도해주세요.", "API", "ERROR");

        register("NT-ERR-G001", "게임 파일 없음", "게임 파일이 아직 생성되지 않았습니다.",
                "미완성 프로젝트의 미리보기를 시도한 경우", "파이프라인이 완료될 때까지 기다려주세요.", "GAME", "WARN");

        register("NT-ERR-S001", "API 키 미설정", "API 키가 설정되지 않았습니다.",
                "API 키 없이 게임 생성을 시도한 경우", "설정 페이지에서 Claude API 키를 등록해주세요.", "SYSTEM", "ERROR");
        register("NT-ERR-S002", "내부 서버 오류", "내부 서버 오류가 발생했습니다.",
                "예상치 못한 서버 에러", "잠시 후 다시 시도해주세요. 지속되면 관리자에게 문의해주세요.", "SYSTEM", "ERROR");
    }

    public static ErrorCodeResponse lookup(String code) {
        return REGISTRY.get(code);
    }

    private static void register(String code, String name, String description,
                                  String cause, String resolution, String category, String severity) {
        REGISTRY.put(code, new ErrorCodeResponse(code, name, description, cause, resolution, category, severity));
    }

    private ErrorCodeRegistry() {
    }
}
