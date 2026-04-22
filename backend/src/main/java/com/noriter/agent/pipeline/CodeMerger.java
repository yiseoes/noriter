package com.noriter.agent.pipeline;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * FRONT + BACK 코드 병합기
 *
 * game.js = BACK(Game 클래스) + FRONT(Renderer 클래스) + 초기화 코드(architecture.json에서)
 * 초기화 코드는 중복 방지를 위해 항상 마지막에 1번만 추가.
 */
@Log4j2
@Component
public class CodeMerger {

    private static final String DEFAULT_INIT_CODE =
            "window.addEventListener('load', () => {\n" +
            "  const canvas = document.getElementById('gameCanvas');\n" +
            "  const renderer = new Renderer();\n" +
            "  renderer.init(canvas);\n" +
            "  const game = new Game(canvas, renderer);\n" +
            "});\n";

    /**
     * 백엔드 로직 + 프론트 렌더링 + 초기화 코드 병합
     * @param architectureJson CTO 아키텍처 JSON (initializationCode 필드 추출용)
     */
    public String mergeGameJs(String backendLogicSection, String frontendRenderSection, String architectureJson) {
        log.info("[코드 병합] game.js 병합 시작 - 백엔드={}자, 프론트={}자",
                backendLogicSection != null ? backendLogicSection.length() : 0,
                frontendRenderSection != null ? frontendRenderSection.length() : 0);

        String initCode = extractInitCode(architectureJson);

        // 백엔드/프론트 코드에서 초기화 코드 중복 제거
        String cleanBackend = removeInitCode(backendLogicSection != null ? backendLogicSection : "");
        String cleanFront = removeInitCode(frontendRenderSection != null ? frontendRenderSection : "");

        StringBuilder merged = new StringBuilder();
        merged.append("// === Game 로직 ===\n");
        merged.append(cleanBackend);
        merged.append("\n\n");
        merged.append("// === Renderer ===\n");
        merged.append(cleanFront);
        merged.append("\n\n");
        merged.append("// === 초기화 ===\n");
        merged.append(initCode);

        String result = deduplicateClasses(merged.toString());
        log.info("[코드 병합] game.js 병합 완료 - 전체 크기={}자", result.length());
        return result;
    }

    /** 하위 호환: architectureJson 없이 호출 시 기본 초기화 코드 사용 */
    public String mergeGameJs(String backendLogicSection, String frontendRenderSection) {
        return mergeGameJs(backendLogicSection, frontendRenderSection, "");
    }

    /**
     * architecture.json의 initializationCode 필드 추출.
     * 없으면 기본 초기화 코드 반환.
     */
    private String extractInitCode(String architectureJson) {
        if (architectureJson == null || architectureJson.isBlank()) {
            return DEFAULT_INIT_CODE;
        }
        try {
            int idx = architectureJson.indexOf("\"initializationCode\"");
            if (idx < 0) return DEFAULT_INIT_CODE;
            int start = architectureJson.indexOf("\"", idx + 20) + 1;
            int end = findJsonStringEnd(architectureJson, start);
            if (start <= 0 || end <= start) return DEFAULT_INIT_CODE;
            String raw = architectureJson.substring(start, end);
            return raw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
        } catch (Exception e) {
            log.warn("[코드 병합] initializationCode 추출 실패, 기본 코드 사용");
            return DEFAULT_INIT_CODE;
        }
    }

    /**
     * window.addEventListener('load', ...) 또는 DOMContentLoaded 초기화 블록 제거.
     * 에이전트가 실수로 포함시킨 중복 초기화 코드 방지.
     */
    private String removeInitCode(String code) {
        // window.addEventListener('load'|'DOMContentLoaded', ...) 패턴 제거
        String result = code.replaceAll(
                "window\\.addEventListener\\s*\\(\\s*['\"](?:load|DOMContentLoaded)['\"].*?\\}\\s*\\);?",
                "/* [초기화 코드는 시스템이 자동 추가] */"
        );
        // const game = new Game() 단독 실행 라인 제거
        result = result.replaceAll("(?m)^\\s*const\\s+game\\s*=\\s*new\\s+Game\\(.*?\\);\\s*$", "");
        return result;
    }

    /**
     * 중복 class 정의 제거 — 같은 클래스가 2번 이상 선언된 경우 마지막 것만 유지.
     * AI가 Renderer 또는 Game 클래스를 두 번 생성하는 케이스 대응.
     */
    private String deduplicateClasses(String code) {
        String result = deduplicateClass(code, "Renderer");
        result = deduplicateClass(result, "Game");
        return result;
    }

    private String deduplicateClass(String code, String className) {
        String marker = "class " + className;
        int firstIdx = code.indexOf(marker);
        int lastIdx = code.lastIndexOf(marker);

        if (firstIdx == lastIdx || firstIdx < 0) {
            return code; // 중복 없음
        }

        // 첫 번째 class 선언부터 두 번째 class 선언 직전까지 제거
        // (첫 번째 블록을 지우고 마지막 블록만 남김)
        String before = code.substring(0, firstIdx);
        String from = code.substring(lastIdx);

        // 구분자 주석도 함께 정리 (바로 앞의 // === ... === 라인 제거)
        String cleaned = (before + from).replaceAll("(?m)^// === " + className + " ===\\s*\\n", "");
        log.warn("[코드 병합] class {} 중복 감지 — 첫 번째 정의 제거 후 마지막만 유지", className);
        return cleaned;
    }

    private int findJsonStringEnd(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == '"') return i;
        }
        return -1;
    }
}
