package com.noriter.service;

import com.noriter.domain.Project;
import com.noriter.domain.Stage;
import com.noriter.domain.User;
import com.noriter.domain.enums.*;
import com.noriter.exception.ErrorCode;
import com.noriter.exception.NoriterException;
import com.noriter.repository.ProjectRepository;
import com.noriter.repository.StageRepository;
import com.noriter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final StageRepository stageRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    private static final int USER_CREATE_LIMIT = 3;
    private static final int FEEDBACK_LIMIT_PER_PROJECT = 2;

    @Value("${noriter.pipeline.max-debug-attempts:3}")
    private int maxDebugAttempts;

    @Transactional
    public Project createProject(String name, String requirement, Genre genre, boolean demo, String guestId, Long userId) {
        log.info("[프로젝트 생성] 시작 - name={}, genre={}, demo={}, guestId={}, userId={}, 요구사항 길이={}자",
                name, genre, demo, guestId, userId, requirement.length());

        // 생성 제한 체크 (ADMIN은 무제한)
        if (!demo && !isAdmin(userId)) {
            if (userId != null) {
                long count = projectRepository.countByUserIdAndDemoFalse(userId);
                if (count >= USER_CREATE_LIMIT) {
                    throw new NoriterException(ErrorCode.USER_CREATE_LIMIT_EXCEEDED);
                }
            } else if (guestId != null) {
                long count = projectRepository.countByGuestIdAndUserIdIsNullAndDemoFalse(guestId);
                if (count >= 1) {
                    throw new NoriterException(ErrorCode.GUEST_LIMIT_EXCEEDED);
                }
            }
        }

        Project project = Project.create(name, requirement, genre, maxDebugAttempts, demo, guestId, userId);
        projectRepository.save(project);
        log.info("[프로젝트 생성] 프로젝트 저장 완료 - id={}", project.getId());

        initializeStages(project);
        log.info("[프로젝트 생성] 스테이지 초기화 완료 - id={}, 스테이지 수={}",
                project.getId(), project.getStages().size());

        auditService.log(AuditEventType.PROJECT_CREATED, project.getId(),
                String.format("프로젝트 '%s' 생성", project.getName()),
                String.format("{\"projectName\":\"%s\",\"genre\":\"%s\"}", name, genre));

        log.info("[프로젝트 생성] 완료 - id={}, status={}", project.getId(), project.getStatus());
        return project;
    }

    public Project getProject(String projectId) {
        log.debug("[프로젝트 조회] id={}", projectId);
        return projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.warn("[프로젝트 조회] 프로젝트를 찾을 수 없음 - id={}", projectId);
                    return new NoriterException(ErrorCode.PROJECT_NOT_FOUND, "id: " + projectId);
                });
    }

    public Project getProjectWithOwnerCheck(String projectId, Long userId, String guestId) {
        Project project = getProject(projectId);
        if (!project.isOwnedBy(userId, guestId)) {
            throw new NoriterException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
        // Lazy 컬렉션 강제 초기화 (open-in-view: false 대응)
        project.getStages().size();
        project.getArtifacts().size();
        return project;
    }

    public Page<Project> getProjects(ProjectStatus status, Pageable pageable, String guestId, Long userId) {
        log.debug("[프로젝트 목록] 조회 시작 - status={}, guestId={}, userId={}, page={}, size={}",
                status, guestId, userId, pageable.getPageNumber(), pageable.getPageSize());

        Page<Project> result;
        if (userId != null) {
            // 로그인 유저: userId 기반 조회
            result = projectRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else if (guestId != null) {
            // 게스트: guestId 기반 조회
            result = projectRepository.findByGuestIdAndUserIdIsNullOrderByCreatedAtDesc(guestId, pageable);
        } else if (status != null) {
            result = projectRepository.findByStatus(status, pageable);
        } else {
            result = projectRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        log.debug("[프로젝트 목록] 조회 완료 - 총 {}건, 현재 페이지 {}건",
                result.getTotalElements(), result.getContent().size());
        return result;
    }

    @Transactional
    public Project retryProject(String projectId, StageType fromStage, Long userId, String guestId) {
        log.info("[프로젝트 재시도] 시작 - id={}, fromStage={}", projectId, fromStage);

        Project project = getProjectWithOwnerCheck(projectId, userId, guestId);
        if (!project.canRetry()) {
            log.warn("[프로젝트 재시도] 재시도 불가 - id={}, 현재 상태={}",
                    projectId, project.getStatus());
            throw new NoriterException(ErrorCode.RETRY_NOT_ALLOWED,
                    "현재 상태: " + project.getStatus());
        }

        project.updateStatus(ProjectStatus.IN_PROGRESS);
        project.resetDebugAttempts();
        log.info("[프로젝트 재시도] 상태 변경 완료 - id={}, IN_PROGRESS, 디버그 카운트 초기화", projectId);

        auditService.log(AuditEventType.USER_ACTION, projectId,
                String.format("프로젝트 재시도 (fromStage: %s)", fromStage), null);

        return project;
    }

    @Transactional
    public void deleteProject(String projectId, Long userId, String guestId) {
        log.info("[프로젝트 삭제] 시작 - id={}", projectId);

        Project project = getProjectWithOwnerCheck(projectId, userId, guestId);
        if (!project.canDelete()) {
            log.warn("[프로젝트 삭제] 삭제 불가 - id={}, 현재 상태={}",
                    projectId, project.getStatus());
            throw new NoriterException(ErrorCode.DELETE_NOT_ALLOWED);
        }

        String projectName = project.getName();
        projectRepository.delete(project);
        log.info("[프로젝트 삭제] 완료 - id={}, name={}", projectId, projectName);

        auditService.log(AuditEventType.PROJECT_DELETED, projectId,
                String.format("프로젝트 '%s' 삭제", projectName), null);
    }

    @Transactional
    public Project cancelProject(String projectId, Long userId, String guestId) {
        log.info("[프로젝트 중단] 시작 - id={}", projectId);

        Project project = getProjectWithOwnerCheck(projectId, userId, guestId);
        if (!project.canCancel()) {
            log.warn("[프로젝트 중단] 중단 불가 - id={}, 현재 상태={}",
                    projectId, project.getStatus());
            throw new NoriterException(ErrorCode.CANCEL_NOT_ALLOWED,
                    "현재 상태: " + project.getStatus());
        }

        project.updateStatus(ProjectStatus.CANCELLED);
        log.info("[프로젝트 중단] 상태 변경 완료 - id={}, CANCELLED", projectId);

        auditService.log(AuditEventType.USER_ACTION, projectId, "프로젝트 중단", null);

        return project;
    }

    @Transactional
    public Project requestFeedback(String projectId, String feedback, Long userId, String guestId) {
        log.info("[수정 요청] 시작 - id={}, 피드백 길이={}자", projectId, feedback.length());

        // 게스트는 수정 불가
        if (userId == null) {
            throw new NoriterException(ErrorCode.GUEST_FEEDBACK_NOT_ALLOWED);
        }

        Project project = getProjectWithOwnerCheck(projectId, userId, guestId);
        if (!project.canFeedback()) {
            throw new NoriterException(ErrorCode.FEEDBACK_NOT_ALLOWED,
                    "현재 상태: " + project.getStatus());
        }

        // 수정 횟수 제한 (ADMIN 무제한)
        if (!isAdmin(userId) && project.getFeedbackCount() >= FEEDBACK_LIMIT_PER_PROJECT) {
            throw new NoriterException(ErrorCode.FEEDBACK_LIMIT_EXCEEDED);
        }

        project.incrementFeedbackCount();
        project.updateStatus(ProjectStatus.REVISION);
        log.info("[수정 요청] 상태 변경 완료 - id={}, REVISION, feedbackCount={}", projectId, project.getFeedbackCount());

        String feedbackJson;
        try {
            feedbackJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(java.util.Map.of("feedback", feedback));
        } catch (Exception e) {
            feedbackJson = "{\"feedback\":\"(인코딩 오류)\"}";
        }
        auditService.log(AuditEventType.USER_ACTION, projectId, "수정 요청 접수", feedbackJson);

        return project;
    }

    private void initializeStages(Project project) {
        log.debug("[스테이지 초기화] 프로젝트 id={}", project.getId());

        List<Stage> stages = List.of(
                Stage.create(project, StageType.PLANNING, "PLANNING", 1),
                Stage.create(project, StageType.ARCHITECTURE, "CTO", 2),
                Stage.create(project, StageType.DESIGN, "DESIGN", 3),
                Stage.create(project, StageType.IMPLEMENTATION, "FRONTEND,BACKEND", 4),
                Stage.create(project, StageType.QA, "QA", 5),
                Stage.create(project, StageType.RELEASE, "SYSTEM", 6)
        );

        stageRepository.saveAll(stages);
        project.getStages().addAll(stages);

        for (Stage stage : stages) {
            log.debug("[스테이지 초기화] {} (순서={}, 담당={}) 생성",
                    stage.getType(), stage.getStageOrder(), stage.getAgentRole());
        }
    }

    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(User::isAdmin)
                .orElse(false);
    }
}
