package com.noriter.agent.pipeline;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * FRONT + BACK 코드 병합기
 * 참조: 08_프롬프트 §12 통합 프로세스
 *
 * STAGE 4 완료 후 PipelineOrchestrator가 호출:
 * game.js = BACK(로직) + FRONT(렌더링) + 초기화 코드
 */
@Log4j2
@Component
public class CodeMerger {

    /**
     * 프론트엔드 렌더링 코드 + 백엔드 게임 로직 코드 → game.js 병합
     */
    public String mergeGameJs(String backendLogicSection, String frontendRenderSection) {
        log.info("[코드 병합] game.js 병합 시작 - 백엔드={}자, 프론트={}자",
                backendLogicSection != null ? backendLogicSection.length() : 0,
                frontendRenderSection != null ? frontendRenderSection.length() : 0);

        StringBuilder merged = new StringBuilder();

        // 1. 백엔드: 게임 로직, 오브젝트 클래스, 상태 관리
        merged.append("// === 게임 로직 (백엔드팀) ===\n");
        if (backendLogicSection != null) {
            merged.append(backendLogicSection);
        }
        merged.append("\n\n");

        // 2. 프론트엔드: 렌더링, 입력 처리, UI, 이펙트
        merged.append("// === 렌더링·UI (프론트팀) ===\n");
        if (frontendRenderSection != null) {
            merged.append(frontendRenderSection);
        }
        merged.append("\n\n");

        // 3. 게임 시작 코드
        merged.append("// === 게임 시작 ===\n");
        merged.append("window.addEventListener('load', () => { const game = new Game(); });\n");

        String result = merged.toString();
        log.info("[코드 병합] game.js 병합 완료 - 전체 크기={}자", result.length());
        return result;
    }
}
