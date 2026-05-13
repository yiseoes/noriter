package com.noriter.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

/**
 * 게임 소스 파일 간 크로스파일 커플링을 감지한다.
 *
 * 감지 항목:
 *   - index.html 저장 시: game.js / style.css 가 참조하는 ID·클래스가 HTML에 존재하는지
 *   - game.js   저장 시: getElementById/querySelector 호출 대상이 index.html에 존재하는지
 *   - style.css  저장 시: CSS 선택자가 index.html의 요소를 타겟하는지
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class GameContractChecker {

    private final FileStorageService fileStorageService;

    public List<String> check(String projectId, String changedFile, String changedContent) {
        List<String> warnings = new ArrayList<>();

        try {
            Map<String, String> others = new HashMap<>();
            for (String name : List.of("index.html", "style.css", "game.js")) {
                if (!name.equals(changedFile)) {
                    String content = fileStorageService.readGameFile(projectId, name);
                    if (content != null) others.put(name, content);
                }
            }

            switch (changedFile) {
                case "index.html" -> checkHtml(changedContent, others, warnings);
                case "game.js"    -> checkJs(changedContent, others, warnings);
                case "style.css"  -> checkCss(changedContent, others, warnings);
            }

            log.info("[커플링 감지] 완료 - projectId={}, file={}, warnings={}건",
                    projectId, changedFile, warnings.size());

        } catch (Exception e) {
            log.warn("[커플링 감지] 오류 - projectId={}, error={}", projectId, e.getMessage());
        }

        return warnings;
    }

    /* ------------------------------------------------------------------ */
    /* index.html 저장 시: game.js·style.css 가 참조하는 ID가 HTML에 있는지 */
    /* ------------------------------------------------------------------ */
    private void checkHtml(String html, Map<String, String> others, List<String> warnings) {
        Set<String> htmlIds = extractHtmlIds(html);
        Set<String> htmlClasses = extractHtmlClasses(html);

        // game.js 가 getElementById/querySelector 로 참조하는 ID 검사
        String gameJs = others.get("game.js");
        if (gameJs != null) {
            for (String id : extractJsReferencedIds(gameJs)) {
                if (!htmlIds.contains(id)) {
                    warnings.add(String.format(
                            "⚠️ game.js에서 getElementById('%s')를 호출하는데, index.html에 id=\"%s\" 요소가 없습니다 → index.html도 함께 수정하세요",
                            id, id));
                }
            }
            for (String cls : extractJsReferencedClasses(gameJs)) {
                if (!htmlClasses.contains(cls)) {
                    warnings.add(String.format(
                            "⚠️ game.js에서 querySelector('.%s')를 사용하는데, index.html에 class=\"%s\" 요소가 없습니다",
                            cls, cls));
                }
            }
        }

        // style.css 가 타겟하는 ID·클래스가 HTML에 있는지 검사
        String css = others.get("style.css");
        if (css != null) {
            for (String id : extractCssIdSelectors(css)) {
                if (!htmlIds.contains(id)) {
                    warnings.add(String.format(
                            "⚠️ style.css에 #%s 선택자가 있는데, index.html에 id=\"%s\" 요소가 없습니다 → index.html도 함께 수정하세요",
                            id, id));
                }
            }
            for (String cls : extractCssClassSelectors(css)) {
                if (!htmlClasses.contains(cls)) {
                    warnings.add(String.format(
                            "⚠️ style.css에 .%s 선택자가 있는데, index.html에 class=\"%s\" 요소가 없습니다 → index.html도 함께 수정하세요",
                            cls, cls));
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* game.js 저장 시: 참조하는 ID가 index.html 에 실제로 존재하는지       */
    /* ------------------------------------------------------------------ */
    private void checkJs(String gameJs, Map<String, String> others, List<String> warnings) {
        String html = others.get("index.html");
        if (html == null) return;

        Set<String> htmlIds = extractHtmlIds(html);
        Set<String> htmlClasses = extractHtmlClasses(html);

        for (String id : extractJsReferencedIds(gameJs)) {
            if (!htmlIds.contains(id)) {
                warnings.add(String.format(
                        "⚠️ game.js에서 getElementById('%s')를 호출하는데, index.html에 id=\"%s\" 요소가 없습니다 → index.html도 함께 수정하세요",
                        id, id));
            }
        }

        for (String cls : extractJsReferencedClasses(gameJs)) {
            if (!htmlClasses.contains(cls)) {
                warnings.add(String.format(
                        "⚠️ game.js에서 querySelector('.%s')를 사용하는데, index.html에 class=\"%s\" 요소가 없습니다 → index.html도 함께 수정하세요",
                        cls, cls));
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* style.css 저장 시: CSS 선택자 대상이 index.html 에 존재하는지         */
    /* ------------------------------------------------------------------ */
    private void checkCss(String css, Map<String, String> others, List<String> warnings) {
        String html = others.get("index.html");
        if (html == null) return;

        Set<String> htmlIds = extractHtmlIds(html);
        Set<String> htmlClasses = extractHtmlClasses(html);

        for (String id : extractCssIdSelectors(css)) {
            if (!htmlIds.contains(id)) {
                warnings.add(String.format(
                        "⚠️ style.css에 #%s 선택자가 있는데, index.html에 id=\"%s\" 요소가 없습니다 → index.html도 함께 수정하세요",
                        id, id));
            }
        }

        for (String cls : extractCssClassSelectors(css)) {
            // body, html 같은 태그 선택자는 제외 — 전역 스타일이라 커플링 경고 불필요
            if (!htmlClasses.contains(cls)) {
                warnings.add(String.format(
                        "⚠️ style.css에 .%s 선택자가 있는데, index.html에 class=\"%s\" 요소가 없습니다 → index.html도 함께 수정하세요",
                        cls, cls));
            }
        }
    }

    /* ===== 파싱 헬퍼 ===== */

    /** index.html 에서 id="..." 값 추출 */
    private Set<String> extractHtmlIds(String html) {
        Set<String> ids = new HashSet<>();
        Matcher m = Pattern.compile("id\\s*=\\s*[\"']([^\"']+)[\"']").matcher(html);
        while (m.find()) ids.add(m.group(1).trim());
        return ids;
    }

    /** index.html 에서 class="..." 의 개별 클래스명 추출 */
    private Set<String> extractHtmlClasses(String html) {
        Set<String> classes = new HashSet<>();
        Matcher m = Pattern.compile("class\\s*=\\s*[\"']([^\"']+)[\"']").matcher(html);
        while (m.find()) {
            for (String cls : m.group(1).trim().split("\\s+")) {
                if (!cls.isEmpty()) classes.add(cls);
            }
        }
        return classes;
    }

    /** game.js 에서 getElementById('id') 로 참조하는 ID 추출 */
    private Set<String> extractJsReferencedIds(String js) {
        Set<String> ids = new HashSet<>();
        Matcher m = Pattern.compile("getElementById\\s*\\(\\s*[\"']([^\"']+)[\"']\\s*\\)").matcher(js);
        while (m.find()) ids.add(m.group(1).trim());
        // querySelector('#id') 형태도 추출
        Matcher m2 = Pattern.compile("querySelector\\s*\\(\\s*[\"']#([\\w-]+)[\"']\\s*\\)").matcher(js);
        while (m2.find()) ids.add(m2.group(1).trim());
        return ids;
    }

    /** game.js 에서 querySelector('.class') 로 참조하는 클래스 추출 */
    private Set<String> extractJsReferencedClasses(String js) {
        Set<String> classes = new HashSet<>();
        Matcher m = Pattern.compile("querySelector\\s*\\(\\s*[\"']\\.([\\w-]+)[\"']\\s*\\)").matcher(js);
        while (m.find()) classes.add(m.group(1).trim());
        return classes;
    }

    /** style.css 에서 #id 선택자 추출 */
    private Set<String> extractCssIdSelectors(String css) {
        Set<String> ids = new HashSet<>();
        Matcher m = Pattern.compile("#([\\w-]+)\\s*[\\{,:\\[]").matcher(css);
        while (m.find()) ids.add(m.group(1).trim());
        return ids;
    }

    /** style.css 에서 .class 선택자 추출 */
    private Set<String> extractCssClassSelectors(String css) {
        Set<String> classes = new HashSet<>();
        Matcher m = Pattern.compile("\\.([\\w-]+)\\s*[\\{,:\\[]").matcher(css);
        while (m.find()) classes.add(m.group(1).trim());
        return classes;
    }
}
