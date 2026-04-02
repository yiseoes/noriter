package com.noriter.agent.prompt;

import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * 프롬프트 템플릿 변수 치환 엔진
 * 참조: 08_프롬프트 §4.2 템플릿 변수
 *
 * {{변수명}} 패턴을 실제 값으로 치환한다.
 */
@Log4j2
public class PromptTemplate {

    private PromptTemplate() {
    }

    /**
     * 템플릿 내 {{변수}} 를 values 맵의 값으로 치환
     */
    public static String render(String template, Map<String, String> values) {
        if (template == null) return "";
        if (values == null || values.isEmpty()) return template;

        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }

        log.debug("[프롬프트 템플릿] 변수 치환 완료 - 치환 변수 {}개, 결과 길이={}자",
                values.size(), result.length());
        return result;
    }
}
