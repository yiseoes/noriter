package com.noriter.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API-PRJ-006: 수정 요청 (피드백)
 * 참조: 05_API §4 API-PRJ-006
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    @NotBlank(message = "수정 요청은 필수입니다")
    @Size(min = 5, message = "수정 요청은 최소 5자 이상 입력해야 합니다")  // NT-ERR-V004
    private String feedback;
}
