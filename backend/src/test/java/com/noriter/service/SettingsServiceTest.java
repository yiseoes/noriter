package com.noriter.service;

import com.noriter.domain.Setting;
import com.noriter.repository.SettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @InjectMocks
    private SettingsService settingsService;

    @Mock
    private SettingRepository settingRepository;

    @Mock
    private AuditService auditService;

    @Test
    @DisplayName("API 키가 설정되지 않았을 때 false를 반환한다")
    void isApiKeyConfigured_whenNotSet_returnsFalse() {
        when(settingRepository.findByKey("claude_api_key")).thenReturn(Optional.empty());

        assertThat(settingsService.isApiKeyConfigured()).isFalse();
    }

    @Test
    @DisplayName("API 키가 설정되어 있으면 true를 반환한다")
    void isApiKeyConfigured_whenSet_returnsTrue() {
        Setting setting = Setting.create("claude_api_key", "sk-ant-test");
        when(settingRepository.findByKey("claude_api_key")).thenReturn(Optional.of(setting));

        assertThat(settingsService.isApiKeyConfigured()).isTrue();
    }

    @Test
    @DisplayName("신규 API 키를 저장한다")
    void saveApiKey_newKey_createsSetting() {
        when(settingRepository.findByKey("claude_api_key")).thenReturn(Optional.empty());
        when(settingRepository.save(any(Setting.class))).thenAnswer(i -> i.getArgument(0));

        Setting result = settingsService.saveApiKey("sk-ant-api03-newkey");

        assertThat(result.getValue()).isEqualTo("sk-ant-api03-newkey");
        verify(settingRepository).save(any(Setting.class));
        verify(auditService).log(any(), any(), any(), any());
    }

    @Test
    @DisplayName("기존 API 키를 업데이트한다")
    void saveApiKey_existingKey_updatesValue() {
        Setting existing = Setting.create("claude_api_key", "sk-ant-old");
        when(settingRepository.findByKey("claude_api_key")).thenReturn(Optional.of(existing));

        Setting result = settingsService.saveApiKey("sk-ant-new");

        assertThat(result.getValue()).isEqualTo("sk-ant-new");
        verify(settingRepository, never()).save(any());
    }

    @Test
    @DisplayName("API 키를 마스킹 처리한다")
    void getMaskedKey_masksCorrectly() {
        String masked = settingsService.getMaskedKey("sk-ant-api03-abcdefgh12345");

        assertThat(masked).startsWith("sk-ant-a");
        assertThat(masked).contains("********");
    }

    @Test
    @DisplayName("짧은 API 키도 마스킹 처리된다")
    void getMaskedKey_shortKey_returnsFullMask() {
        String masked = settingsService.getMaskedKey("short");

        assertThat(masked).isEqualTo("********");
    }
}
