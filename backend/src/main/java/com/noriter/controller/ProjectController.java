package com.noriter.controller;

import com.noriter.controller.dto.request.CreateProjectRequest;
import com.noriter.controller.dto.request.FeedbackRequest;
import com.noriter.controller.dto.request.RetryProjectRequest;
import com.noriter.controller.dto.response.*;
import com.noriter.domain.Artifact;
import com.noriter.domain.Project;
import com.noriter.domain.Stage;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.Genre;
import com.noriter.domain.enums.ProjectStatus;
import com.noriter.domain.enums.StageType;
import com.noriter.exception.ErrorCode;
import com.noriter.exception.NoriterException;
import com.noriter.service.ArtifactService;
import com.noriter.service.PipelineService;
import com.noriter.service.ProjectService;
import com.noriter.service.SettingsService;
import com.noriter.service.TokenUsageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 프로젝트 관리 API
 * 참조: 05_API §3 API-PRJ-001~007 (7개 엔드포인트)
 */
@Log4j2
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final PipelineService pipelineService;
    private final ArtifactService artifactService;
    private final TokenUsageService tokenUsageService;
    private final SettingsService settingsService;

    /**
     * API-PRJ-001: 게임 생성 요청
     * POST /api/projects
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        log.info("[API-PRJ-001] 게임 생성 요청 수신 - name={}, genre={}, demo={}, guestId={}", request.getName(), request.getGenre(), request.isDemo(), guestId);

        if (!request.isDemo() && !settingsService.isApiKeyConfigured()) {
            log.warn("[API-PRJ-001] API 키 미설정 상태에서 생성 요청");
            throw new NoriterException(ErrorCode.API_KEY_NOT_CONFIGURED);
        }

        Genre genre = parseGenre(request.getGenre());
        String name = request.getName() != null ? request.getName() : "새 게임 프로젝트";

        Project project = projectService.createProject(name, request.getRequirement(), genre, request.isDemo(), guestId);
        log.info("[API-PRJ-001] 게임 생성 완료 - id={}", project.getId());

        // 파이프라인 자동 시작 (비동기)
        pipelineService.startPipeline(project.getId());
        log.info("[API-PRJ-001] 파이프라인 시작 요청됨 - id={}", project.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(new ProjectResponse(project));
    }

    /**
     * API-PRJ-002: 프로젝트 목록 조회
     * GET /api/projects
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProjectResponse>> getProjects(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        log.info("[API-PRJ-002] 프로젝트 목록 조회 - status={}, page={}, size={}, guestId={}", status, page, size, guestId);

        ProjectStatus projectStatus = status != null ? ProjectStatus.valueOf(status) : null;
        Page<Project> projects = projectService.getProjects(
                projectStatus, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")), guestId);

        List<ProjectResponse> content = projects.getContent().stream()
                .map(ProjectResponse::new).toList();

        log.info("[API-PRJ-002] 목록 조회 완료 - 총 {}건", projects.getTotalElements());
        return ResponseEntity.ok(new PageResponse<>(projects, content));
    }

    /**
     * API-PRJ-003: 프로젝트 상세 조회
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailResponse> getProjectDetail(@PathVariable String id) {
        log.info("[API-PRJ-003] 프로젝트 상세 조회 - id={}", id);

        Project project = projectService.getProject(id);
        List<StageResponse> stages = project.getStages().stream()
                .map(StageResponse::new).toList();
        List<ArtifactResponse> artifacts = artifactService.getArtifacts(id).stream()
                .map(ArtifactResponse::new).toList();
        long totalTokens = tokenUsageService.getTotalTokens(id);
        Map<AgentRole, Long> tokensByAgent = tokenUsageService.getTokensByAgent(id);

        log.info("[API-PRJ-003] 상세 조회 완료 - id={}, status={}, 산출물 {}건",
                id, project.getStatus(), artifacts.size());

        return ResponseEntity.ok(new ProjectDetailResponse(project, stages, artifacts, totalTokens, tokensByAgent));
    }

    /**
     * API-PRJ-004: 프로젝트 재시도
     * POST /api/projects/{id}/retry
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<ActionResponse> retryProject(
            @PathVariable String id, @RequestBody(required = false) RetryProjectRequest request) {
        log.info("[API-PRJ-004] 프로젝트 재시도 요청 - id={}, fromStage={}",
                id, request != null ? request.getFromStage() : "처음부터");

        StageType fromStage = null;
        if (request != null && request.getFromStage() != null) {
            fromStage = StageType.valueOf(request.getFromStage());
        }

        Project project = projectService.retryProject(id, fromStage);
        String message = fromStage != null
                ? fromStage + " 스테이지부터 재시도합니다."
                : "처음부터 재시도합니다.";

        log.info("[API-PRJ-004] 재시도 시작 - id={}, fromStage={}", id, fromStage);
        return ResponseEntity.ok(new ActionResponse(project.getId(), project.getStatus().name(), message));
    }

    /**
     * API-PRJ-005: 프로젝트 삭제
     * DELETE /api/projects/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        log.info("[API-PRJ-005] 프로젝트 삭제 요청 - id={}", id);

        projectService.deleteProject(id);

        log.info("[API-PRJ-005] 프로젝트 삭제 완료 - id={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * API-PRJ-006: 수정 요청 (피드백)
     * POST /api/projects/{id}/feedback
     */
    @PostMapping("/{id}/feedback")
    public ResponseEntity<ActionResponse> requestFeedback(
            @PathVariable String id, @Valid @RequestBody FeedbackRequest request) {
        log.info("[API-PRJ-006] 수정 요청 수신 - id={}, 피드백 길이={}자", id, request.getFeedback().length());

        Project project = projectService.requestFeedback(id, request.getFeedback());

        log.info("[API-PRJ-006] 수정 요청 접수 완료 - id={}, status={}", id, project.getStatus());
        return ResponseEntity.ok(new ActionResponse(
                project.getId(), project.getStatus().name(),
                "수정 요청이 접수되었습니다. CTO가 수정 범위를 판단 중입니다."));
    }

    /**
     * API-PRJ-007: 파이프라인 중단
     * POST /api/projects/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ActionResponse> cancelProject(@PathVariable String id) {
        log.info("[API-PRJ-007] 파이프라인 중단 요청 - id={}", id);

        Project project = projectService.cancelProject(id);

        log.info("[API-PRJ-007] 파이프라인 중단 완료 - id={}", id);
        return ResponseEntity.ok(new ActionResponse(
                project.getId(), project.getStatus().name(),
                "파이프라인이 중단되었습니다. 기존 산출물은 보존됩니다."));
    }

    private Genre parseGenre(String genre) {
        if (genre == null || genre.isBlank()) {
            return null;
        }
        try {
            return Genre.valueOf(genre);
        } catch (IllegalArgumentException e) {
            log.warn("[장르 파싱] 유효하지 않은 장르 - genre={}", genre);
            throw new NoriterException(ErrorCode.INVALID_GENRE, genre);
        }
    }
}
