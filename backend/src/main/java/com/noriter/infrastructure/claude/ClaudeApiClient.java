package com.noriter.infrastructure.claude;

import com.noriter.agent.core.AgentConfig;
import com.noriter.domain.enums.AgentRole;
import com.noriter.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Claude API 클라이언트
 * 참조: 03_아키텍처 §5.1 ClaudeApiClient
 *
 * - sendPrompt(): 동기 호출, 응답 텍스트 반환
 * - 재시도 정책 내장 (NT-AGT-005: 3회, 지수 백오프 1초→2초→4초)
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class ClaudeApiClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final int MAX_RETRIES = 3;
    private static final Duration TIMEOUT = Duration.ofSeconds(120);

    private final SettingsService settingsService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * Claude API 호출 — 동기
     * 참조: 03_아키텍처 §5.1 sendPrompt()
     *
     * @param systemPrompt 시스템 프롬프트
     * @param userPrompt   유저 프롬프트 (템플릿 변수 치환 완료 상태)
     * @param agentRole    호출 에이전트 (설정 조회용)
     * @return Claude 응답 텍스트
     */
    public ClaudeResponse sendPrompt(String systemPrompt, String userPrompt, AgentRole agentRole) {
        AgentConfig config = AgentConfig.forRole(agentRole);

        log.info("[Claude API] 호출 시작 - agent={}, model={}, temperature={}, maxTokens={}",
                agentRole, config.getModel(), config.getTemperature(), config.getMaxTokens());

        String apiKey = getApiKey();

        String requestBody = buildRequestBody(systemPrompt, userPrompt, config);
        log.debug("[Claude API] 요청 본문 크기={}자", requestBody.length());

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.debug("[Claude API] 시도 {}/{} - agent={}", attempt, MAX_RETRIES, agentRole);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(TIMEOUT)
                        .header("Content-Type", "application/json")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", API_VERSION)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                log.debug("[Claude API] 응답 수신 - statusCode={}, 응답 크기={}자",
                        statusCode, response.body().length());

                if (statusCode == 200) {
                    ClaudeResponse parsed = parseResponse(response.body());
                    log.info("[Claude API] 호출 성공 - agent={}, inputTokens={}, outputTokens={}",
                            agentRole, parsed.inputTokens(), parsed.outputTokens());
                    return parsed;
                }

                if (statusCode == 429) {
                    log.warn("[Claude API] Rate Limit 도달 - agent={}, 시도 {}/{}", agentRole, attempt, MAX_RETRIES);
                    if (attempt < MAX_RETRIES) {
                        backoff(attempt);
                        continue;
                    }
                    throw ClaudeApiException.rateLimited();
                }

                if (statusCode == 401) {
                    log.error("[Claude API] 인증 실패 - agent={}", agentRole);
                    throw ClaudeApiException.authFailed();
                }

                log.warn("[Claude API] 예상치 못한 응답 - statusCode={}, body={}",
                        statusCode, response.body().substring(0, Math.min(200, response.body().length())));

                if (attempt < MAX_RETRIES) {
                    backoff(attempt);
                } else {
                    throw new ClaudeApiException("NT-ERR-A004",
                            "Claude API 호출 실패 (HTTP " + statusCode + ")");
                }

            } catch (ClaudeApiException e) {
                throw e;
            } catch (java.net.http.HttpTimeoutException e) {
                log.warn("[Claude API] 타임아웃 - agent={}, 시도 {}/{}", agentRole, attempt, MAX_RETRIES);
                if (attempt >= MAX_RETRIES) {
                    throw ClaudeApiException.timeout();
                }
                backoff(attempt);
            } catch (Exception e) {
                log.error("[Claude API] 예외 발생 - agent={}, 시도 {}/{}, error={}",
                        agentRole, attempt, MAX_RETRIES, e.getMessage());
                if (attempt >= MAX_RETRIES) {
                    throw new ClaudeApiException("NT-ERR-A004", "Claude API 호출 실패: " + e.getMessage(), e);
                }
                backoff(attempt);
            }
        }

        throw new ClaudeApiException("NT-ERR-A004", "Claude API 호출 실패 (최대 재시도 초과)");
    }

    /**
     * API 키 유효성 검증 — 실제 API 호출로 확인
     */
    public boolean validateApiKey(String apiKey) {
        log.info("[Claude API] API 키 유효성 검증 시작");

        try {
            String requestBody = """
                    {"model":"claude-sonnet-4-6","max_tokens":10,"messages":[{"role":"user","content":"hi"}]}
                    """;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", API_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean valid = response.statusCode() == 200;

            log.info("[Claude API] API 키 검증 결과 - valid={}, statusCode={}", valid, response.statusCode());
            return valid;

        } catch (Exception e) {
            log.warn("[Claude API] API 키 검증 중 오류 - error={}", e.getMessage());
            return false;
        }
    }

    private String getApiKey() {
        return settingsService.getApiKeySetting()
                .map(s -> s.getValue())
                .orElseThrow(() -> {
                    log.error("[Claude API] API 키 미설정 상태에서 호출 시도");
                    return new ClaudeApiException("NT-ERR-S001", "API 키가 설정되지 않았습니다.");
                });
    }

    private String buildRequestBody(String systemPrompt, String userPrompt, AgentConfig config) {
        // JSON 이스케이프 처리
        String escapedSystem = escapeJson(systemPrompt);
        String escapedUser = escapeJson(userPrompt);

        return String.format("""
                {"model":"%s","max_tokens":%d,"temperature":%.1f,"system":"%s","messages":[{"role":"user","content":"%s"}]}
                """, config.getModel(), config.getMaxTokens(), config.getTemperature(),
                escapedSystem, escapedUser);
    }

    private ClaudeResponse parseResponse(String responseBody) {
        // 간단한 JSON 파싱 (Jackson 사용 시 교체 예정)
        String content = extractJsonField(responseBody, "text");
        int inputTokens = extractJsonInt(responseBody, "input_tokens");
        int outputTokens = extractJsonInt(responseBody, "output_tokens");

        return new ClaudeResponse(content, inputTokens, outputTokens);
    }

    private void backoff(int attempt) {
        long waitMs = (long) Math.pow(2, attempt - 1) * 1000;  // 1초, 2초, 4초
        log.debug("[Claude API] 백오프 대기 - {}ms", waitMs);
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractJsonField(String json, String field) {
        String searchKey = "\"" + field + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return "";
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
    }

    private int extractJsonInt(String json, String field) {
        String searchKey = "\"" + field + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return 0;
        start += searchKey.length();
        StringBuilder num = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c)) num.append(c);
            else if (!num.isEmpty()) break;
        }
        return num.isEmpty() ? 0 : Integer.parseInt(num.toString());
    }

    public record ClaudeResponse(String content, int inputTokens, int outputTokens) {
    }
}
