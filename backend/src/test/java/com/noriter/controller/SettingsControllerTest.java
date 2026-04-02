package com.noriter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noriter.controller.dto.request.ApiKeyRequest;
import com.noriter.domain.Setting;
import com.noriter.service.SettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettingsController.class)
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SettingsService settingsService;

    @Test
    @DisplayName("API-SET-001: API 키 미설정 시 configured=false를 반환한다")
    void getApiKey_notConfigured() throws Exception {
        when(settingsService.getApiKeySetting()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/settings/api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.maskedKey").doesNotExist());
    }

    @Test
    @DisplayName("API-SET-001: API 키 설정됨 시 마스킹된 키를 반환한다")
    void getApiKey_configured() throws Exception {
        Setting setting = Setting.create("claude_api_key", "sk-ant-api03-test12345");
        when(settingsService.getApiKeySetting()).thenReturn(Optional.of(setting));
        when(settingsService.getMaskedKey(anyString())).thenReturn("sk-ant-a********...****");

        mockMvc.perform(get("/api/settings/api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.maskedKey").value("sk-ant-a********...****"));
    }

    @Test
    @DisplayName("API-SET-002: API 키를 설정할 수 있다")
    void setApiKey_success() throws Exception {
        Setting setting = Setting.create("claude_api_key", "sk-ant-api03-newkey");
        when(settingsService.saveApiKey(anyString())).thenReturn(setting);
        when(settingsService.getMaskedKey(anyString())).thenReturn("sk-ant-a********...****");

        ApiKeyRequest request = new ApiKeyRequest("sk-ant-api03-newkey");

        mockMvc.perform(put("/api/settings/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true));
    }

    @Test
    @DisplayName("API-SET-003: 유효한 API 키 검증 시 valid=true를 반환한다")
    void validateApiKey_valid() throws Exception {
        ApiKeyRequest request = new ApiKeyRequest("sk-ant-api03-validkey");

        mockMvc.perform(post("/api/settings/api-key/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.model").value("claude-sonnet-4-6"));
    }

    @Test
    @DisplayName("API-SYS-001: 헬스체크가 정상 응답한다")
    void healthCheck_returnsUp() throws Exception {
        when(settingsService.isApiKeyConfigured()).thenReturn(true);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.server.status").value("UP"))
                .andExpect(jsonPath("$.components.claudeApi.configured").value(true));
    }
}
