package com.noriter.controller;

import com.noriter.controller.dto.request.CreateProjectRequest;
import com.noriter.controller.dto.request.FeedbackRequest;
import com.noriter.controller.dto.request.RetryProjectRequest;
import com.noriter.controller.dto.response.*;
import com.noriter.domain.Project;
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
import jakarta.servlet.http.HttpServletRequest;
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
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            HttpServletRequest httpRequest) {

        Long userId = getUserId(httpRequest);
        log.info("[API-PRJ-001] 게임 생성 요청 수신 - name={}, genre={}, demo={}, guestId={}, userId={}",
                request.getName(), request.getGenre(), request.isDemo(), guestId, userId);

        if (!request.isDemo() && !settingsService.isApiKeyConfigured()) {
            throw new NoriterException(ErrorCode.API_KEY_NOT_CONFIGURED);
        }

        Genre genre = parseGenre(request.getGenre());
        String name = request.getName() != null ? request.getName() : "새 게임 프로젝트";

        Project project = projectService.createProject(name, request.getRequirement(), genre, request.isDemo(), guestId, userId);
        log.info("[API-PRJ-001] 게임 생성 완료 - id={}", project.getId());

        pipelineService.startPipeline(project.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProjectResponse(project));
    }

    /**
     * API-PRJ-002: 프로젝트 목록 조회
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProjectResponse>> getProjects(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            HttpServletRequest httpRequest) {

        Long userId = getUserId(httpRequest);
        log.info("[API-PRJ-002] 프로젝트 목록 조회 - status={}, guestId={}, userId={}", status, guestId, userId);

        ProjectStatus projectStatus = status != null ? ProjectStatus.valueOf(status) : null;
        Page<Project> projects = projectService.getProjects(
                projectStatus, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")), guestId, userId);

        List<ProjectResponse> content = projects.getContent().stream()
                .map(ProjectResponse::new).toList();

        return ResponseEntity.ok(new PageResponse<>(projects, content));
    }

    /**
     * API-PRJ-003: 프로젝트 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailResponse> getProjectDetail(
            @PathVariable String id,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            HttpServletRequest httpRequest) {

        Long userId = getUserId(httpRequest);
        Project project = projectService.getProjectWithOwnerCheck(id, userId, guestId);

        List<StageResponse> stages = project.getStages().stream()
                .map(StageResponse::new).toList();
        List<ArtifactResponse> artifacts = artifactService.getArtifacts(id).stream()
                .map(ArtifactResponse::new).toList();
        long totalTokens = tokenUsageService.getTotalTokens(id);
        Map<AgentRole, Long> tokensByAgent = tokenUsageService.getTokensByAgent(id);

        return ResponseEntity.ok(new ProjectDetailResponse(project, stages, artifacts, totalTokens, tokensByAgent));
    }

    /**
     * API-PRJ-004: 프로젝트 재시도
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<ActionResponse> retryProject(
            @PathVariable String id,
            @RequestBody(required = false) RetryProjectRequest request,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            HttpServletRequest httpRequest) {

        Long userId = getUserId(httpRequest);
        StageType fromStage = null;
        if (request != null && request.getFromStage() != null) {
            fromStage = StageType.valueOf(request.getFromStage());
        }

        Project project = projectService.retryProject(id, fromStage, userId, guestId);

        // 파이프라인 재시작 (fromStage 있으면 이어서, 없으면 처음부터)
        if (fromStage != null) {
            pipelineService.resumePipeline(id, fromStage);
        } else {
            pipelineService.startPipeline(id);
        }

        String message = fromStage != null
                ? fromStage + " 스테이지부터 재시도합니다."
                : "처음부터 재시도합니다.";

        return ResponseEntity.ok(new ActionResponse(project.getId(), project.getStatus().name(), message));
    }

    /**
     * API-PRJ-005: 프로젝트 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable String id,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            HttpServletRequest httpRequest) {

        Long userId = getUserId(httpRequest);
        projectService.deleteProject(id, userId, guestId);
        return ResponseEntity.noContent().build();
    }

    /**
     * API-PRJ-006: 수정 요청 (피드백)
     */
    @PostMapping("/{id}/feedback")
    public ResponseEntity<ActionResponse> requestFeedback(
            @PathVariable String id,
            @Valid @RequestBody FeedbackRequest request,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            HttpServletRequest httpRequest) {

        Long userId = getUserId(httpRequest);
        Project project = projectService.requestFeedback(id, request.getFeedback(), userId, guestId);
        pipelineService.startRevisionPipeline(project.getId(), request.getFeedback());

        return ResponseEntity.ok(new ActionResponse(
                project.getId(), project.getStatus().name(),
                "수정 요청이 접수되었습니다. CTO가 수정 범위를 판단 중입니다."));
    }

    /**
     * API-PRJ-007: 파이프라인 중단
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ActionResponse> cancelProject(
            @PathVariable String id,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            HttpServletRequest httpRequest) {

        Long userId = getUserId(httpRequest);
        Project project = projectService.cancelProject(id, userId, guestId);

        return ResponseEntity.ok(new ActionResponse(
                project.getId(), project.getStatus().name(),
                "파이프라인이 중단되었습니다. 기존 산출물은 보존됩니다."));
    }

    /** JWT 필터가 세팅한 userId attribute 추출 (비로그인이면 null) */
    private Long getUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        return attr instanceof Long ? (Long) attr : null;
    }

    private Genre parseGenre(String genre) {
        if (genre == null || genre.isBlank()) return null;
        try {
            return Genre.valueOf(genre);
        } catch (IllegalArgumentException e) {
            throw new NoriterException(ErrorCode.INVALID_GENRE, genre);
        }
    }
}
