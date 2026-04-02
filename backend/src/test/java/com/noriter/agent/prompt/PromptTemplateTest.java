package com.noriter.agent.prompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplateTest {

    @Test
    @DisplayName("템플릿 변수를 올바르게 치환한다")
    void render_replacesVariables() {
        String template = "요구사항: {{requirement}}\n장르: {{genre}}";
        Map<String, String> values = Map.of(
                "requirement", "뱀파이어 서바이벌 게임",
                "genre", "ACTION"
        );

        String result = PromptTemplate.render(template, values);

        assertThat(result).contains("뱀파이어 서바이벌 게임");
        assertThat(result).contains("ACTION");
        assertThat(result).doesNotContain("{{");
    }

    @Test
    @DisplayName("null 값은 빈 문자열로 치환한다")
    void render_handlesNullValue() {
        String template = "장르: {{genre}}";
        Map<String, String> values = Map.of();

        String result = PromptTemplate.render(template, values);

        assertThat(result).isEqualTo("장르: {{genre}}");
    }

    @Test
    @DisplayName("null 템플릿은 빈 문자열을 반환한다")
    void render_handlesNullTemplate() {
        String result = PromptTemplate.render(null, Map.of("key", "value"));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null 값 맵이면 원본 템플릿을 그대로 반환한다")
    void render_handlesNullValues() {
        String template = "hello {{name}}";
        String result = PromptTemplate.render(template, null);
        assertThat(result).isEqualTo("hello {{name}}");
    }

    @Test
    @DisplayName("여러 변수를 동시에 치환한다")
    void render_replacesMultipleVariables() {
        String template = "{{plan}} + {{architecture}} → {{design}}";
        Map<String, String> values = Map.of(
                "plan", "기획서",
                "architecture", "아키텍처",
                "design", "디자인"
        );

        String result = PromptTemplate.render(template, values);

        assertThat(result).isEqualTo("기획서 + 아키텍처 → 디자인");
    }
}
