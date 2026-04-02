package com.noriter.service;

import com.noriter.agent.pipeline.PipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * 파이프라인 서비스 — Controller에서 파이프라인 시작을 위임받음
 * 참조: 03_아키텍처 §6 service/PipelineService
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineOrchestrator pipelineOrchestrator;

    public void startPipeline(String projectId) {
        log.info("[파이프라인 서비스] 파이프라인 시작 요청 - projectId={}", projectId);
        pipelineOrchestrator.startPipeline(projectId);
        log.info("[파이프라인 서비스] 파이프라인 비동기 시작됨 - projectId={}", projectId);
    }
}
