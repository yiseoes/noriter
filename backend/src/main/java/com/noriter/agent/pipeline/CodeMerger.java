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
            "  const hiddenInput = document.getElementById('hiddenInput');\n" +
            "  const renderer = new Renderer();\n" +
            "  renderer.init(canvas);\n" +
            "  const game = new Game(canvas, renderer);\n" +
            "  renderer.setGame(game);\n" +
            "\n" +
            "  // iframe 환경에서 키보드 포커스 확보\n" +
            "  canvas.setAttribute('tabindex', '0');\n" +
            "  canvas.focus();\n" +
            "\n" +
            "  // 부모 페이지에서 postMessage로 전달된 키 이벤트 수신\n" +
            "  window.addEventListener('message', (e) => {\n" +
            "    if (e.data && e.data.type === 'keydown') {\n" +
            "      game.handleKeyDown(e.data);\n" +
            "    }\n" +
            "  });\n" +
            "\n" +
            "  canvas.addEventListener('click', (e) => {\n" +
            "    const rect = canvas.getBoundingClientRect();\n" +
            "    const scaleX = canvas.width / rect.width;\n" +
            "    const scaleY = canvas.height / rect.height;\n" +
            "    game.handleClick((e.clientX - rect.left) * scaleX, (e.clientY - rect.top) * scaleY);\n" +
            "  });\n" +
            "\n" +
            "  document.addEventListener('keydown', (e) => {\n" +
            "    game.handleKeyDown(e);\n" +
            "  });\n" +
            "\n" +
            "  // 한글 IME 입력 처리 (input/compositionend 리스너는 Game 생성자에서 등록)\n" +
            "  let lastTime = 0;\n" +
            "  function gameLoop(timestamp) {\n" +
            "    const dt = Math.min((timestamp - lastTime) / 1000, 0.05);\n" +
            "    lastTime = timestamp;\n" +
            "    game.update(dt);\n" +
            "    renderer.render(game);\n" +
            "    requestAnimationFrame(gameLoop);\n" +
            "  }\n" +
            "  requestAnimationFrame((timestamp) => {\n" +
            "    lastTime = timestamp;\n" +
            "    requestAnimationFrame(gameLoop);\n" +
            "  });\n" +
            "\n" +
            "  window.addEventListener('resize', () => {\n" +
            "    renderer.resizeCanvas();\n" +
            "  });\n" +
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

        // 백엔드: 초기화 코드 + Renderer 클래스 제거 (AI가 실수로 포함시키는 케이스 대응)
        String cleanBackend = removeInitCode(backendLogicSection != null ? backendLogicSection : "");
        cleanBackend = stripClass(cleanBackend, "Renderer");

        // 프론트: 초기화 코드 + Game 클래스 제거
        String cleanFront = removeInitCode(frontendRenderSection != null ? frontendRenderSection : "");
        cleanFront = stripClass(cleanFront, "Game");

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
     * 특정 클래스 정의를 코드에서 제거.
     * 중괄호 depth 추적으로 클래스 블록 전체를 정확히 제거.
     * AI가 지시를 무시하고 불필요한 클래스를 포함시키는 케이스 대응.
     */
    /**
     * game.js에서 특정 클래스 블록만 추출 (역추출용)
     */
    public String extractClass(String code, String className) {
        String marker = "class " + className;
        int classIdx = code.indexOf(marker);
        if (classIdx < 0) return null;
        int braceStart = code.indexOf('{', classIdx);
        if (braceStart < 0) return null;
        int depth = 0;
        int braceEnd = -1;
        for (int i = braceStart; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) { braceEnd = i; break; }
            }
        }
        if (braceEnd < 0) return null;
        return code.substring(classIdx, braceEnd + 1).trim();
    }

    private String stripClass(String code, String className) {
        String marker = "class " + className;
        int classIdx = code.indexOf(marker);
        if (classIdx < 0) return code; // 해당 클래스 없음 — 정상

        int braceStart = code.indexOf('{', classIdx);
        if (braceStart < 0) return code;

        // 매칭되는 닫는 중괄호 찾기
        int depth = 0;
        int braceEnd = -1;
        for (int i = braceStart; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) { braceEnd = i; break; }
            }
        }
        if (braceEnd < 0) return code;

        // 클래스 선언 앞의 섹션 주석 라인도 함께 제거
        String before = code.substring(0, classIdx)
                .replaceAll("(?m)^//[^\n]*" + className + "[^\n]*\n$", "");
        String after = code.substring(braceEnd + 1);
        log.warn("[코드 병합] {} 클래스 제거 — BackendAgent/FrontendAgent가 잘못 포함시킨 클래스", className);
        return (before.stripTrailing() + "\n" + after.stripLeading()).trim();
    }

    /**
     * 중복 class 정의 제거 (안전망 — stripClass 이후에도 남은 중복 처리).
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
        if (firstIdx == lastIdx || firstIdx < 0) return code;

        // 첫 번째 클래스 블록을 stripClass로 정확히 제거
        String stripped = stripClass(code.substring(firstIdx), className);
        String result = code.substring(0, firstIdx) + "\n" + stripped;
        log.warn("[코드 병합] class {} 중복 감지 — 첫 번째 정의 제거 후 마지막만 유지", className);
        return result;
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
