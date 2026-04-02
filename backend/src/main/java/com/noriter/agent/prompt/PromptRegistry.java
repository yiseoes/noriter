package com.noriter.agent.prompt;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 프롬프트 레지스트리 — 프롬프트 ID별 System/User 템플릿 관리
 * 참조: 08_프롬프트 §2 프롬프트 ID 체계, 09_디렉토리 §3 agent/prompt/
 *
 * templates/ 디렉토리의 txt 파일을 로드하여 캐싱한다.
 * 파일 없으면 하드코딩된 기본 템플릿 사용.
 */
@Log4j2
@Component
public class PromptRegistry {

    private final Map<String, String> templates = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("[프롬프트 레지스트리] 초기화 시작");

        String[] promptIds = {
                "plan-main", "cto-main", "cto-debug", "cto-feedback",
                "design-main", "front-main", "front-fix",
                "back-main", "back-fix", "qa-main", "qa-retest"
        };

        for (String id : promptIds) {
            loadTemplate(id + "-system");
            loadTemplate(id + "-user");
        }

        log.info("[프롬프트 레지스트리] 초기화 완료 - 로드된 템플릿 {}개", templates.size());
    }

    /**
     * 시스템 프롬프트 조회
     */
    public String getSystemPrompt(String promptId) {
        return templates.getOrDefault(promptId + "-system", "");
    }

    /**
     * 유저 프롬프트 조회
     */
    public String getUserPrompt(String promptId) {
        return templates.getOrDefault(promptId + "-user", "");
    }

    /**
     * 프롬프트 존재 여부 확인
     */
    public boolean exists(String promptId) {
        return templates.containsKey(promptId + "-system");
    }

    private void loadTemplate(String name) {
        String path = "templates/" + name + ".txt";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                templates.put(name, content);
                log.debug("[프롬프트 레지스트리] 템플릿 로드 성공 - {}, 크기={}자", name, content.length());
            } else {
                log.debug("[프롬프트 레지스트리] 템플릿 파일 없음 (추후 생성 예정) - {}", path);
            }
        } catch (IOException e) {
            log.warn("[프롬프트 레지스트리] 템플릿 로드 실패 - {}, error={}", path, e.getMessage());
        }
    }
}
