package com.noriter.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 에이전트 응답 JSON 파싱·검증
 * 참조: 08_프롬프트 §17 에러 처리 정책 — JSON 파싱 실패 시 재요청
 */
@Log4j2
public class JsonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonParser() {
    }

    /**
     * JSON 문자열을 파싱하여 유효한지 검증
     * @return 파싱된 JsonNode, 실패 시 null
     */
    public static JsonNode parse(String json) {
        if (json == null || json.isBlank()) {
            log.warn("[JSON 파싱] 입력이 비어있음");
            return null;
        }

        // Claude 응답에서 마크다운 코드블록 래퍼 제거
        String cleaned = stripCodeBlock(json);

        try {
            JsonNode node = MAPPER.readTree(cleaned);
            log.debug("[JSON 파싱] 성공 - 필드 수={}", node.size());
            return node;
        } catch (JsonProcessingException e) {
            log.warn("[JSON 파싱] 실패 - error={}, 입력 앞부분={}",
                    e.getMessage(), cleaned.substring(0, Math.min(100, cleaned.length())));
            return null;
        }
    }

    /**
     * JSON 문자열이 유효한지 검증
     */
    public static boolean isValid(String json) {
        return parse(json) != null;
    }

    /**
     * 특정 필드가 존재하고 비어있지 않은지 검증
     */
    public static boolean hasField(JsonNode node, String fieldName) {
        if (node == null) return false;
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() && !field.asText().isBlank();
    }

    /**
     * JSON 문자열을 Map<String, String>으로 파싱
     * 최상위 필드의 값을 문자열로 추출 (중첩 객체는 toString)
     */
    public static Map<String, String> parseAsMap(String json) {
        Map<String, String> result = new HashMap<>();
        JsonNode node = parse(json);
        if (node == null || !node.isObject()) return result;

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                result.put(entry.getKey(), value.asText());
            } else {
                result.put(entry.getKey(), value.toString());
            }
        }
        return result;
    }

    /**
     * 마크다운 코드블록 래퍼 제거
     * Claude가 ```json ... ``` 으로 감싸서 출력하는 경우 대응
     */
    public static String stripCodeBlock(String text) {
        if (text == null) return null;
        String trimmed = text.trim();

        // ```json ... ``` 제거
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed.trim();
    }
}
