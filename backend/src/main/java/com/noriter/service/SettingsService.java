package com.noriter.service;

import com.noriter.domain.Setting;
import com.noriter.domain.enums.AuditEventType;
import com.noriter.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String API_KEY_SETTING = "claude_api_key";

    private final SettingRepository settingRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Optional<Setting> getApiKeySetting() {
        log.debug("[API 키 조회] 시작");
        Optional<Setting> setting = settingRepository.findByKey(API_KEY_SETTING);

        if (setting.isPresent()) {
            log.debug("[API 키 조회] API 키 설정됨 - updatedAt={}", setting.get().getUpdatedAt());
        } else {
            log.debug("[API 키 조회] API 키 미설정 상태");
        }
        return setting;
    }

    public boolean isApiKeyConfigured() {
        boolean configured = settingRepository.findByKey(API_KEY_SETTING).isPresent();
        log.debug("[API 키 상태 확인] configured={}", configured);
        return configured;
    }

    @Transactional
    public Setting saveApiKey(String apiKey) {
        log.info("[API 키 저장] 시작 - 키 앞 8자리={}", maskKey(apiKey));

        Optional<Setting> existing = settingRepository.findByKey(API_KEY_SETTING);
        Setting setting;

        if (existing.isPresent()) {
            setting = existing.get();
            setting.updateValue(apiKey);
            log.info("[API 키 저장] 기존 키 업데이트 완료");
        } else {
            setting = Setting.create(API_KEY_SETTING, apiKey);
            settingRepository.save(setting);
            log.info("[API 키 저장] 신규 키 저장 완료 - id={}", setting.getId());
        }

        auditService.log(AuditEventType.SETTING_CHANGED, null,
                "Claude API 키 변경", null);

        return setting;
    }

    public String getMaskedKey(String apiKey) {
        return maskKey(apiKey);
    }

    private String maskKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "********";
        }
        return apiKey.substring(0, 8) + "********...****";
    }
}
