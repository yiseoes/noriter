package com.noriter.service;

import com.noriter.agent.pipeline.PipelineOrchestrator;
import com.noriter.domain.enums.StageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineOrchestrator pipelineOrchestrator;

    public void startPipeline(String projectId) {
        log.info("[파이프라인 서비스] 파이프라인 시작 요청 - projectId={}", projectId);
        pipelineOrchestrator.startPipeline(projectId);
    }

    public void resumePipeline(String projectId, StageType fromStage) {
        log.info("[파이프라인 서비스] 파이프라인 재시도 요청 - projectId={}, fromStage={}", projectId, fromStage);
        pipelineOrchestrator.resumePipeline(projectId, fromStage);
    }
}
