package com.noriter.controller;

import com.noriter.exception.ErrorCode;
import com.noriter.exception.NoriterException;
import com.noriter.infrastructure.storage.FileStorageService;
import com.noriter.infrastructure.storage.FileStorageService.GameFileInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.noriter.auth.JwtAuthenticationFilter;
import com.noriter.auth.JwtUtil;
import com.noriter.auth.SecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("API-GAM-001: 게임 미리보기 성공 시 HTML을 반환한다")
    void getPreview_success_returnsHtml() throws Exception {
        String html = "<!DOCTYPE html><html><body>게임</body></html>";
        when(fileStorageService.getPreviewHtml("prj_test")).thenReturn(html);

        mockMvc.perform(get("/api/projects/prj_test/preview"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(html));
    }

    @Test
    @DisplayName("API-GAM-001: 게임 파일 없으면 404를 반환한다")
    void getPreview_noFiles_returns404() throws Exception {
        when(fileStorageService.getPreviewHtml("prj_none")).thenReturn(null);

        mockMvc.perform(get("/api/projects/prj_none/preview"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-G001"));
    }

    @Test
    @DisplayName("API-GAM-002: 소스 코드 목록을 반환한다")
    void getSourceFiles_returnsList() throws Exception {
        List<GameFileInfo> files = List.of(
                new GameFileInfo("index.html", 2048, "html"),
                new GameFileInfo("style.css", 1024, "css"),
                new GameFileInfo("game.js", 8192, "javascript")
        );
        when(fileStorageService.getGameFiles("prj_test")).thenReturn(files);

        mockMvc.perform(get("/api/projects/prj_test/source"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.files.length()").value(3))
                .andExpect(jsonPath("$.files[0].path").value("index.html"))
                .andExpect(jsonPath("$.files[0].type").value("html"))
                .andExpect(jsonPath("$.files[2].type").value("javascript"));
    }

    @Test
    @DisplayName("API-GAM-003: 특정 소스 파일 내용을 반환한다")
    void getSourceFile_returnsContent() throws Exception {
        when(fileStorageService.readGameFile("prj_test", "game.js"))
                .thenReturn("class Game { constructor() {} }");

        mockMvc.perform(get("/api/projects/prj_test/source/game.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string("class Game { constructor() {} }"));
    }

    @Test
    @DisplayName("API-GAM-003: 없는 소스 파일 요청 시 404를 반환한다")
    void getSourceFile_notFound_returns404() throws Exception {
        when(fileStorageService.readGameFile("prj_test", "missing.js")).thenReturn(null);

        mockMvc.perform(get("/api/projects/prj_test/source/missing.js"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-G001"));
    }

    @Test
    @DisplayName("API-GAM-004: 게임 ZIP 다운로드 성공")
    void downloadGame_success_returnsZip() throws Exception {
        byte[] fakeZip = new byte[]{0x50, 0x4B, 0x03, 0x04};  // ZIP 매직 넘버
        when(fileStorageService.packageAsZip("prj_test")).thenReturn(fakeZip);

        mockMvc.perform(get("/api/projects/prj_test/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"game_prj_test.zip\""));
    }

    @Test
    @DisplayName("API-GAM-004: 게임 파일 없으면 다운로드 시 404를 반환한다")
    void downloadGame_noFiles_returns404() throws Exception {
        when(fileStorageService.packageAsZip("prj_none")).thenReturn(null);

        mockMvc.perform(get("/api/projects/prj_none/download"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-G001"));
    }
}
