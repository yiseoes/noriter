package com.noriter.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonParserTest {

    @Test
    @DisplayName("유효한 JSON을 파싱한다")
    void parse_validJson_returnsNode() {
        JsonNode node = JsonParser.parse("{\"gameName\":\"테스트\",\"genre\":\"액션\"}");

        assertThat(node).isNotNull();
        assertThat(node.get("gameName").asText()).isEqualTo("테스트");
    }

    @Test
    @DisplayName("잘못된 JSON은 null을 반환한다")
    void parse_invalidJson_returnsNull() {
        assertThat(JsonParser.parse("이건 JSON이 아닙니다")).isNull();
        assertThat(JsonParser.parse("{잘못된")).isNull();
    }

    @Test
    @DisplayName("null/빈 문자열은 null을 반환한다")
    void parse_nullOrEmpty_returnsNull() {
        assertThat(JsonParser.parse(null)).isNull();
        assertThat(JsonParser.parse("")).isNull();
        assertThat(JsonParser.parse("   ")).isNull();
    }

    @Test
    @DisplayName("마크다운 코드블록 래퍼를 제거한다")
    void stripCodeBlock_removesWrapper() {
        String wrapped = "```json\n{\"key\":\"value\"}\n```";
        String stripped = JsonParser.stripCodeBlock(wrapped);

        assertThat(stripped).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    @DisplayName("코드블록 래핑된 JSON도 파싱한다")
    void parse_wrappedJson_succeeds() {
        String wrapped = "```json\n{\"result\":\"PASS\"}\n```";
        JsonNode node = JsonParser.parse(wrapped);

        assertThat(node).isNotNull();
        assertThat(node.get("result").asText()).isEqualTo("PASS");
    }

    @Test
    @DisplayName("isValid로 유효성을 검증한다")
    void isValid_checksCorrectly() {
        assertThat(JsonParser.isValid("{\"ok\":true}")).isTrue();
        assertThat(JsonParser.isValid("not json")).isFalse();
    }

    @Test
    @DisplayName("hasField로 필드 존재를 확인한다")
    void hasField_checksCorrectly() {
        JsonNode node = JsonParser.parse("{\"gameName\":\"테스트\",\"empty\":\"\"}");

        assertThat(JsonParser.hasField(node, "gameName")).isTrue();
        assertThat(JsonParser.hasField(node, "missing")).isFalse();
        assertThat(JsonParser.hasField(node, "empty")).isFalse();
        assertThat(JsonParser.hasField(null, "any")).isFalse();
    }
}
