package com.noriter.util;

import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaScript 코드 정적 검증기
 *
 * AI가 생성한 game.js의 구조적 오류를 QaAgent 실행 전에 빠르게 감지.
 * 실제 JS 실행 없이 텍스트 파싱으로 주요 오류 패턴을 잡는다.
 */
@Log4j2
public class JsSyntaxValidator {

    public record ValidationResult(boolean valid, List<String> errors) {
        public static ValidationResult ok() {
            return new ValidationResult(true, List.of());
        }
        public static ValidationResult fail(List<String> errors) {
            return new ValidationResult(false, errors);
        }
    }

    /**
     * game.js 기본 구조 검증
     */
    public static ValidationResult validate(String gameJs) {
        if (gameJs == null || gameJs.isBlank()) {
            return ValidationResult.fail(List.of("game.js가 비어 있습니다."));
        }

        List<String> errors = new ArrayList<>();

        // 1. 중괄호 균형 검사
        checkBraces(gameJs, errors);

        // 2. 필수 클래스 존재 확인
        if (!gameJs.contains("class Game")) {
            errors.add("Game 클래스가 없습니다.");
        }
        if (!gameJs.contains("class Renderer")) {
            errors.add("Renderer 클래스가 없습니다.");
        }

        // 3. 초기화 코드 중복 감지 (load/DOMContentLoaded 이벤트만 카운트 — 클릭/터치/키보드 이벤트는 정상)
        long initCount = countOccurrences(gameJs, "addEventListener('load'")
                + countOccurrences(gameJs, "addEventListener(\"load\"")
                + countOccurrences(gameJs, "addEventListener('DOMContentLoaded'")
                + countOccurrences(gameJs, "addEventListener(\"DOMContentLoaded\"");
        if (initCount > 1) {
            errors.add("초기화 addEventListener('load')가 " + initCount + "번 중복 — 초기화 코드 중복 의심.");
        }

        // 3-1. 클래스 중복 감지
        long gameClassCount = countOccurrences(gameJs, "class Game");
        if (gameClassCount > 1) {
            errors.add("class Game이 " + gameClassCount + "번 중복 선언되어 있습니다.");
        }
        long rendererClassCount = countOccurrences(gameJs, "class Renderer");
        if (rendererClassCount > 1) {
            errors.add("class Renderer가 " + rendererClassCount + "번 중복 선언되어 있습니다.");
        }

        // 4. 코드 잘림 감지 (응답이 truncated 된 경우 마지막이 불완전)
        String trimmed = gameJs.stripTrailing();
        if (!trimmed.isEmpty()) {
            char last = trimmed.charAt(trimmed.length() - 1);
            if (last != '}' && last != ';' && last != ')') {
                errors.add("코드가 잘린 것 같습니다. 마지막 문자: '" + last + "'");
            }
        }

        // 5. JSON 래핑 잔재 감지
        if (gameJs.contains("\"gameJs\":") || gameJs.startsWith("{\"")) {
            errors.add("JSON 래핑이 제거되지 않았습니다. game.js가 JSON string으로 감싸져 있습니다.");
        }

        if (errors.isEmpty()) {
            log.info("[JS검증] 통과");
            return ValidationResult.ok();
        } else {
            log.warn("[JS검증] {} 오류 발견: {}", errors.size(), errors);
            return ValidationResult.fail(errors);
        }
    }

    private static void checkBraces(String code, List<String> errors) {
        int curly = 0, square = 0, paren = 0;
        boolean inString = false;
        char stringChar = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            char next = i + 1 < code.length() ? code.charAt(i + 1) : 0;

            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') { inBlockComment = false; i++; }
                continue;
            }
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == stringChar) inString = false;
                continue;
            }

            if (c == '/' && next == '/') { inLineComment = true; i++; continue; }
            if (c == '/' && next == '*') { inBlockComment = true; i++; continue; }
            if (c == '"' || c == '\'' || c == '`') { inString = true; stringChar = c; continue; }

            if (c == '{') curly++;
            else if (c == '}') curly--;
            else if (c == '[') square++;
            else if (c == ']') square--;
            else if (c == '(') paren++;
            else if (c == ')') paren--;
        }

        if (curly != 0) errors.add("중괄호 {} 불균형: " + (curly > 0 ? "+" : "") + curly);
        if (square != 0) errors.add("대괄호 [] 불균형: " + (square > 0 ? "+" : "") + square);
        if (paren != 0) errors.add("소괄호 () 불균형: " + (paren > 0 ? "+" : "") + paren);
    }

    private static long countOccurrences(String text, String keyword) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) { count++; idx += keyword.length(); }
        return count;
    }
}
