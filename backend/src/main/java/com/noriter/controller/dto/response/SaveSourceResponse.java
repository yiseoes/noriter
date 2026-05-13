package com.noriter.controller.dto.response;

import java.util.List;

public record SaveSourceResponse(List<String> warnings, List<SaveSourceResponse.Fix> fixes) {

    /**
     * 자동 수정 항목: 특정 파일에서 from → to 로 일괄 치환
     */
    public record Fix(String file, String from, String to, String description) {}
}
