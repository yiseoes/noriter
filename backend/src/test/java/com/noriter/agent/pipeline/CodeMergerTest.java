package com.noriter.agent.pipeline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeMergerTest {

    private final CodeMerger codeMerger = new CodeMerger();

    @Test
    @DisplayName("백엔드 로직과 프론트 렌더링을 병합한다")
    void mergeGameJs_combinesBothSections() {
        String backendLogic = "class Game { constructor() {} }";
        String frontendRender = "class Renderer { draw() {} }";

        String merged = codeMerger.mergeGameJs(backendLogic, frontendRender);

        assertThat(merged).contains("게임 로직 (백엔드팀)");
        assertThat(merged).contains("렌더링·UI (프론트팀)");
        assertThat(merged).contains("class Game");
        assertThat(merged).contains("class Renderer");
        assertThat(merged).contains("게임 시작");
        assertThat(merged).contains("window.addEventListener");
    }

    @Test
    @DisplayName("백엔드 코드가 프론트 코드보다 먼저 위치한다 (08_프롬프트 §12)")
    void mergeGameJs_backendComesFirst() {
        String merged = codeMerger.mergeGameJs("BACKEND_CODE", "FRONTEND_CODE");

        int backendPos = merged.indexOf("BACKEND_CODE");
        int frontendPos = merged.indexOf("FRONTEND_CODE");

        assertThat(backendPos).isLessThan(frontendPos);
    }

    @Test
    @DisplayName("null 코드도 안전하게 병합한다")
    void mergeGameJs_handlesNull() {
        String merged = codeMerger.mergeGameJs(null, null);

        assertThat(merged).contains("게임 시작");
        assertThat(merged).contains("window.addEventListener");
    }
}
