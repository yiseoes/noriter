package com.noriter.controller;

import com.noriter.controller.dto.request.ApiKeyRequest;
import com.noriter.controller.dto.response.ApiKeyResponse;
import com.noriter.controller.dto.response.ApiKeyValidationResponse;
import com.noriter.controller.dto.response.HealthResponse;
import com.noriter.domain.Setting;
import com.noriter.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 설정 API
 * 참조: 05_API §3 API-SET-001~003, API-SYS-001
 */
@Log4j2
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * API-SET-001: API 키 조회 (마스킹)
     * GET /api/settings/api-key
     */
    @GetMapping("/settings/api-key")
    public ResponseEntity<ApiKeyResponse> getApiKey() {
        log.info("[API-SET-001] API 키 조회 요청");

        Optional<Setting> setting = settingsService.getApiKeySetting();

        if (setting.isPresent()) {
            String maskedKey = settingsService.getMaskedKey(setting.get().getValue());
            log.info("[API-SET-001] API 키 조회 완료 - 설정됨, maskedKey={}", maskedKey);
            return ResponseEntity.ok(new ApiKeyResponse(true, maskedKey, setting.get().getUpdatedAt()));
        }

        log.info("[API-SET-001] API 키 미설정 상태");
        return ResponseEntity.ok(new ApiKeyResponse(false, null, null));
    }

    /**
     * API-SET-002: API 키 설정/변경
     * PUT /api/settings/api-key
     */
    @PutMapping("/settings/api-key")
    public ResponseEntity<ApiKeyResponse> setApiKey(@Valid @RequestBody ApiKeyRequest request) {
        log.info("[API-SET-002] API 키 설정 요청");

        Setting setting = settingsService.saveApiKey(request.getApiKey());
        String maskedKey = settingsService.getMaskedKey(setting.getValue());

        log.info("[API-SET-002] API 키 설정 완료 - maskedKey={}", maskedKey);
        return ResponseEntity.ok(new ApiKeyResponse(true, maskedKey, setting.getUpdatedAt()));
    }

    /**
     * API-SET-003: API 키 유효성 검증
     * POST /api/settings/api-key/validate
     */
    @PostMapping("/settings/api-key/validate")
    public ResponseEntity<ApiKeyValidationResponse> validateApiKey(@Valid @RequestBody ApiKeyRequest request) {
        log.info("[API-SET-003] API 키 유효성 검증 요청");

        // TODO: 실제 Claude API 호출로 검증 (ClaudeApiClient 구현 후)
        boolean valid = request.getApiKey() != null && request.getApiKey().startsWith("sk-ant-");
        String model = valid ? "claude-sonnet-4-6" : null;
        String message = valid ? "API 키가 유효합니다." : "API 키가 유효하지 않습니다. 키를 확인해주세요.";

        log.info("[API-SET-003] 검증 결과 - valid={}", valid);
        return ResponseEntity.ok(new ApiKeyValidationResponse(valid, model, message));
    }

    /**
     * API-SYS-001: 시스템 헬스체크
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> healthCheck() {
        log.debug("[API-SYS-001] 헬스체크 요청");

        Map<String, Object> components = new LinkedHashMap<>();
        components.put("server", Map.of("status", "UP"));
        components.put("database", Map.of("status", "UP")); // TODO: 실제 DB 연결 확인
        components.put("claudeApi", Map.of(
                "status", settingsService.isApiKeyConfigured() ? "UP" : "DOWN",
                "configured", settingsService.isApiKeyConfigured()));
        components.put("activePipelines", Map.of("count", 0)); // TODO: 실제 파이프라인 수

        log.debug("[API-SYS-001] 헬스체크 완료");
        return ResponseEntity.ok(new HealthResponse("UP", components, LocalDateTime.now()));
    }
}
