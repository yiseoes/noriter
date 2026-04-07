package com.noriter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noriter.auth.JwtAuthenticationFilter;
import com.noriter.auth.JwtUtil;
import com.noriter.auth.SecurityConfig;
import com.noriter.controller.dto.request.CreateProjectRequest;
import com.noriter.controller.dto.request.FeedbackRequest;
import com.noriter.domain.Project;
import com.noriter.domain.enums.Genre;
import com.noriter.domain.enums.ProjectStatus;
import com.noriter.exception.ErrorCode;
import com.noriter.exception.NoriterException;
import com.noriter.service.ArtifactService;
import com.noriter.service.PipelineService;
import com.noriter.service.ProjectService;
import com.noriter.service.SettingsService;
import com.noriter.service.TokenUsageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private PipelineService pipelineService;

    @MockitoBean
    private ArtifactService artifactService;

    @MockitoBean
    private TokenUsageService tokenUsageService;

    @MockitoBean
    private SettingsService settingsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("API-PRJ-001: 게임 생성 요청 성공 시 201을 반환한다")
    void createProject_success_returns201() throws Exception {
        Project project = Project.create("테스트 게임", "뱀파이어 서바이벌 미니게임 만들어줘", Genre.ACTION, 3, false, null, null);
        when(settingsService.isApiKeyConfigured()).thenReturn(true);
        when(projectService.createProject(anyString(), anyString(), any(), anyBoolean(), any(), any())).thenReturn(project);

        CreateProjectRequest request = new CreateProjectRequest("테스트 게임", "뱀파이어 서바이벌 미니게임 만들어줘", "ACTION", false);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.name").value("테스트 게임"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("API-PRJ-001: 요구사항 10자 미만이면 400을 반환한다")
    void createProject_shortRequirement_returns400() throws Exception {
        when(settingsService.isApiKeyConfigured()).thenReturn(true);
        CreateProjectRequest request = new CreateProjectRequest("게임", "짧은거", null, false);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API-PRJ-001: API 키 미설정 시 503을 반환한다")
    void createProject_noApiKey_returns503() throws Exception {
        when(settingsService.isApiKeyConfigured()).thenReturn(false);
        CreateProjectRequest request = new CreateProjectRequest("게임", "뱀파이어 서바이벌 미니게임 만들어줘", null, false);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-S001"));
    }

    @Test
    @DisplayName("API-PRJ-003: 프로젝트 상세 조회 성공")
    void getProjectDetail_success() throws Exception {
        Project project = Project.create("게임", "요구사항 10자 이상입니다", Genre.ACTION, 3, false, null, null);
        when(projectService.getProjectWithOwnerCheck(eq(project.getId()), any(), any())).thenReturn(project);
        when(artifactService.getArtifacts(project.getId())).thenReturn(Collections.emptyList());
        when(tokenUsageService.getTotalTokens(project.getId())).thenReturn(0L);
        when(tokenUsageService.getTokensByAgent(project.getId())).thenReturn(Map.of());

        mockMvc.perform(get("/api/projects/" + project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.debugAttempts").value(0))
                .andExpect(jsonPath("$.maxDebugAttempts").value(3));
    }

    @Test
    @DisplayName("API-PRJ-003: 존재하지 않는 프로젝트 조회 시 404를 반환한다")
    void getProjectDetail_notFound_returns404() throws Exception {
        when(projectService.getProjectWithOwnerCheck(eq("prj_invalid"), any(), any()))
                .thenThrow(new NoriterException(ErrorCode.PROJECT_NOT_FOUND, "id: prj_invalid"));

        mockMvc.perform(get("/api/projects/prj_invalid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-P001"));
    }

    @Test
    @DisplayName("API-PRJ-005: 프로젝트 삭제 성공 시 204를 반환한다")
    void deleteProject_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/projects/prj_test1234"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("API-PRJ-005: 진행 중인 프로젝트 삭제 시 409를 반환한다")
    void deleteProject_inProgress_returns409() throws Exception {
        doThrow(new NoriterException(ErrorCode.DELETE_NOT_ALLOWED))
                .when(projectService).deleteProject(eq("prj_test1234"), any(), any());

        mockMvc.perform(delete("/api/projects/prj_test1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-P003"));
    }

    @Test
    @DisplayName("API-PRJ-006: 수정 요청 성공")
    void requestFeedback_success() throws Exception {
        Project project = Project.create("게임", "요구사항 10자 이상입니다", Genre.ACTION, 3, false, null, null);
        project.updateStatus(ProjectStatus.COMPLETED);
        project.updateStatus(ProjectStatus.REVISION);
        when(projectService.requestFeedback(eq(project.getId()), anyString(), any(), any())).thenReturn(project);

        FeedbackRequest request = new FeedbackRequest("난이도를 낮춰주세요");

        mockMvc.perform(post("/api/projects/" + project.getId() + "/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVISION"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("API-PRJ-007: 파이프라인 중단 성공")
    void cancelProject_success() throws Exception {
        Project project = Project.create("게임", "요구사항 10자 이상입니다", Genre.ACTION, 3, false, null, null);
        project.updateStatus(ProjectStatus.CANCELLED);
        when(projectService.cancelProject(eq(project.getId()), any(), any())).thenReturn(project);

        mockMvc.perform(post("/api/projects/" + project.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
