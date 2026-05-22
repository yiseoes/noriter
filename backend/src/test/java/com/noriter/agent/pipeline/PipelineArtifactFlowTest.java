package com.noriter.agent.pipeline;

import com.noriter.agent.core.AgentContext;
import com.noriter.agent.core.AgentResult;
import com.noriter.agent.impl.*;
import com.noriter.agent.message.MessageBus;
import com.noriter.domain.Project;
import com.noriter.domain.Stage;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.StageType;
import com.noriter.infrastructure.sse.SseEmitterService;
import com.noriter.infrastructure.storage.FileStorageService;
import com.noriter.repository.ProjectRepository;
import com.noriter.repository.StageRepository;
import com.noriter.service.ArtifactService;
import com.noriter.service.AuditService;
import com.noriter.service.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PipelineOrchestrator 아티팩트 흐름 통합 검증
 *
 * 정적 문자열 검색이 아닌 실제 파이프라인을 Mock 에이전트로 실행하여,
 * 각 에이전트가 받는 AgentContext.previousArtifacts가 올바른지 검증한다.
 *
 * 테스트 시나리오:
 * 1. startPipeline — BackendAgent가 content.json을 받는지
 * 2. startPipeline — QA FAIL → CTO debug가 test-report.json을 bugReport로 받는지
 * 3. resumePipeline(fromOrder=4) — 파일에서 로드된 content.json이 BackendAgent에 전달되는지
 * 4. startRevisionPipeline — QA FAIL → CTO가 test-report.json을 bugReport로 받는지
 * 5. debug loop — fix 후 gameJsLogicSection/gameJsRenderSection이 재병합에 올바르게 사용되는지
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PipelineOrchestrator 아티팩트 흐름 검증")
class PipelineArtifactFlowTest {

    // ── 오케스트레이터 의존성 ───────────────────────────────────────────────
    @InjectMocks PipelineOrchestrator orchestrator;

    @Mock ProjectRepository   projectRepository;
    @Mock StageRepository     stageRepository;
    @Mock ArtifactService     artifactService;
    @Mock FileStorageService  fileStorageService;
    @Mock StageExecutor       stageExecutor;
    @Mock CodeMerger          codeMerger;
    @Mock AuditService        auditService;
    @Mock LogService          logService;
    @Mock SseEmitterService   sseEmitterService;
    @Mock MessageBus          messageBus;

    // ── 에이전트 Mock ───────────────────────────────────────────────────────
    @Mock PlanningAgent  planningAgent;
    @Mock ContentAgent   contentAgent;
    @Mock CtoAgent       ctoAgent;
    @Mock DesignAgent    designAgent;
    @Mock FrontendAgent  frontendAgent;
    @Mock BackendAgent   backendAgent;
    @Mock QaAgent        qaAgent;

    // ── 테스트용 도메인 객체 ───────────────────────────────────────────────
    private Project       project;
    private List<Stage>   stages;

    // JS 검증기(JsSyntaxValidator/JsRuntimeValidator)를 통과하는 최소 game.js
    private static final String VALID_GAME_JS =
            "class Game {\n" +
            "  constructor(canvas, renderer) {}\n" +
            "  update(dt) {}\n" +
            "  getGameState() { return { state: 'start' }; }\n" +
            "  handleClick(x, y) {}\n" +
            "  handleKeyDown(e) {}\n" +
            "}\n" +
            "class Renderer {\n" +
            "  constructor() {}\n" +
            "  init(canvas) {}\n" +
            "  render(game) {}\n" +
            "  shake(m, d) {}\n" +
            "  setGame(game) { this.game = game; }\n" +
            "  resizeCanvas() {}\n" +
            "}\n";

    @BeforeEach
    void setUp() {
        project = Project.create("test-game", "테스트 게임", null, 3, false, null, null);
        stages  = createTestStages(project);

        // 기본 Repository/Service 스텁
        lenient().when(projectRepository.findById(any())).thenReturn(Optional.of(project));
        lenient().when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(stageRepository.findByProjectIdOrderByStageOrderAsc(any())).thenReturn(stages);
        lenient().when(stageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(fileStorageService.saveArtifact(any(), any(), any())).thenReturn("path");
        lenient().when(fileStorageService.saveGameFile(any(), any(), any())).thenReturn("path");

        // 에이전트 역할 스텁 (StageExecutor.getAgentDisplayName 내부 호출에 필요)
        lenient().when(planningAgent.getRole()).thenReturn(AgentRole.PLANNING);
        lenient().when(contentAgent.getRole()).thenReturn(AgentRole.CONTENT);
        lenient().when(ctoAgent.getRole()).thenReturn(AgentRole.CTO);
        lenient().when(designAgent.getRole()).thenReturn(AgentRole.DESIGN);
        lenient().when(backendAgent.getRole()).thenReturn(AgentRole.BACKEND);
        lenient().when(frontendAgent.getRole()).thenReturn(AgentRole.FRONTEND);
        lenient().when(qaAgent.getRole()).thenReturn(AgentRole.QA);

        // CodeMerger 기본 스텁
        lenient().when(codeMerger.mergeGameJs(any(), any(), any())).thenReturn(VALID_GAME_JS);
        lenient().when(codeMerger.extractClass(any(), eq("Renderer"))).thenReturn("class Renderer {}");
        lenient().when(codeMerger.extractClass(any(), eq("Game"))).thenReturn("class Game {}");

        // BackendAgent.executeFix 기본 스텁 (JsRuntimeValidator 재시도 대비)
        lenient().when(backendAgent.executeFix(any())).thenReturn(
                AgentResult.success(Map.of("gameJsLogicSection", "class Game {}"), "", 0, 0));
    }

    // =========================================================================
    // 시나리오 1: startPipeline — content.json이 BackendAgent에 전달되는지
    // =========================================================================

    @Test
    @DisplayName("[시나리오 1] startPipeline: BackendAgent가 content.json을 previousArtifacts에서 받아야 한다")
    void startPipeline_backendAgent_receivesContentJson() {
        // given
        setupNormalPipeline();

        ArgumentCaptor<AgentContext> backendCaptor = ArgumentCaptor.forClass(AgentContext.class);
        when(stageExecutor.executeAgent(eq(backendAgent), backendCaptor.capture(), any()))
                .thenReturn(success("gameJsLogicSection", "class Game {}"));

        // when
        orchestrator.startPipeline(project.getId());

        // then
        AgentContext ctx = backendCaptor.getValue();
        assertThat(ctx.getPreviousArtifacts())
                .as("""
                    BackendAgent.execute()에 content.json이 없음.
                    → 단어장/스테이지/아이템 데이터 없이 게임 로직 생성됨.
                    원인: ContentAgent 결과가 artifacts에 putAll된 후 BackendAgent context에 전달되어야 함.
                    """)
                .containsKey("content.json");

        assertThat(ctx.getPreviousArtifacts().get("content.json"))
                .as("content.json 값이 ContentAgent가 생성한 값과 일치해야 함")
                .isEqualTo("단어장 데이터");
    }

    // =========================================================================
    // 시나리오 2: startPipeline QA FAIL — CTO가 test-report.json을 bugReport로 받는지
    // =========================================================================

    @Test
    @DisplayName("[시나리오 2] startPipeline: QA FAIL → CTO executeDebug가 test-report.json을 bugReport로 받아야 한다")
    void startPipeline_qaFail_ctoReceivesTestReportAsBugReport() {
        // given
        setupNormalPipeline();
        String testReportJson = "{\"result\":\"FAIL\",\"bugs\":[{\"severity\":\"HIGH\",\"description\":\"test\"}]}";
        when(stageExecutor.executeAgent(eq(qaAgent), any(), any()))
                .thenReturn(AgentResult.needsReview(Map.of("test-report.json", testReportJson), "bugs", 0, 0));

        ArgumentCaptor<AgentContext> ctoDebugCaptor = ArgumentCaptor.forClass(AgentContext.class);
        when(ctoAgent.executeDebug(ctoDebugCaptor.capture()))
                .thenReturn(success("debug-instruction.json", "{}"));
        when(frontendAgent.executeFix(any()))
                .thenReturn(success("gameJsRenderSection", "class Renderer {}"));
        // QA retest (직접 호출) → PASS로 루프 종료
        when(qaAgent.execute(any()))
                .thenReturn(success("test-report.json", "{\"result\":\"PASS\"}"));

        // when
        orchestrator.startPipeline(project.getId());

        // then
        AgentContext ctoCtx = ctoDebugCaptor.getValue();
        assertThat(ctoCtx.getBugReport())
                .as("""
                    CTO executeDebug의 bugReport가 비어 있음.
                    원인: QA NEEDS_REVIEW 처리 전에 qaResult.getArtifacts()가 artifacts에 putAll되어야 함.
                    test-report.json이 없으면 CTO가 빈 버그 목록으로 수정 지시를 작성함.
                    """)
                .isNotBlank();

        assertThat(ctoCtx.getBugReport())
                .as("bugReport에 test-report.json의 실제 내용이 포함되어야 함")
                .contains("FAIL");
    }

    // =========================================================================
    // 시나리오 3: resumePipeline(fromOrder=4) — 파일에서 로드한 content.json이 BackendAgent에 전달되는지
    // =========================================================================

    @Test
    @DisplayName("[시나리오 3] resumePipeline(IMPLEMENTATION부터): loadExistingArtifacts의 content.json이 BackendAgent에 전달되어야 한다")
    void resumePipeline_fromImplementation_backendReceivesContentJsonFromFile() {
        // given: 파일 저장소에 content.json이 있는 상황
        when(fileStorageService.readArtifact(any(), eq("content.json")))
                .thenReturn("저장된 콘텐츠 데이터");
        when(fileStorageService.readArtifact(any(), eq("plan.json")))
                .thenReturn("{\"title\":\"test\"}");
        when(fileStorageService.readArtifact(any(), eq("architecture.json")))
                .thenReturn("{\"arch\":true}");
        when(fileStorageService.readArtifact(any(), eq("design.json")))
                .thenReturn("{\"design\":true}");
        when(fileStorageService.readGameFile(any(), eq("game.js")))
                .thenReturn(VALID_GAME_JS);
        // index.html, style.css → mock 기본값 null로 반환됨 (stub 불필요)
        // codeMerger.extractClass → setUp의 lenient 스텁이 already 커버 (any(), eq("Game/Renderer"))

        ArgumentCaptor<AgentContext> backendCaptor = ArgumentCaptor.forClass(AgentContext.class);
        when(stageExecutor.executeAgent(eq(backendAgent), backendCaptor.capture(), any()))
                .thenReturn(success("gameJsLogicSection", "class Game {}"));
        when(stageExecutor.executeAgent(eq(frontendAgent), any(), any()))
                .thenReturn(success("gameJsRenderSection", "class Renderer {}"));
        when(stageExecutor.executeAgent(eq(qaAgent), any(), any()))
                .thenReturn(success("test-report.json", "{\"result\":\"PASS\"}"));

        // when: IMPLEMENTATION(4)부터 재시작
        orchestrator.resumePipeline(project.getId(), StageType.IMPLEMENTATION);

        // then
        AgentContext ctx = backendCaptor.getValue();
        assertThat(ctx.getPreviousArtifacts())
                .as("""
                    resumePipeline(fromStage=IMPLEMENTATION)에서 BackendAgent에 content.json이 없음.
                    원인: loadExistingArtifacts()의 artifactFiles 배열에 "content.json"이 없거나
                          fileStorageService.readArtifact가 content.json을 반환하지 않음.
                    결과: 단어장/스테이지 데이터 없이 게임 로직이 재생성됨.
                    """)
                .containsKey("content.json");

        assertThat(ctx.getPreviousArtifacts().get("content.json"))
                .as("파일에서 로드된 content.json 값이 BackendAgent에 전달되어야 함")
                .isEqualTo("저장된 콘텐츠 데이터");
    }

    // =========================================================================
    // 시나리오 4: startRevisionPipeline QA FAIL — CTO가 test-report.json을 받는지
    // =========================================================================

    @Test
    @DisplayName("[시나리오 4] startRevisionPipeline: QA FAIL → CTO executeDebug가 test-report.json을 bugReport로 받아야 한다")
    void startRevisionPipeline_qaFail_ctoReceivesTestReport() {
        // given
        setupRevisionPipelineBase();
        String testReportJson = "{\"result\":\"FAIL\",\"bugs\":[{\"severity\":\"CRITICAL\"}]}";
        when(qaAgent.execute(any()))
                .thenReturn(AgentResult.needsReview(Map.of("test-report.json", testReportJson), "fail", 0, 0))
                .thenReturn(success("test-report.json", "{\"result\":\"PASS\"}"));  // retest → PASS

        ArgumentCaptor<AgentContext> ctoDebugCaptor = ArgumentCaptor.forClass(AgentContext.class);
        when(ctoAgent.executeDebug(ctoDebugCaptor.capture()))
                .thenReturn(success("debug-instruction.json", "{}"));
        when(frontendAgent.executeFix(any()))
                .thenReturn(success("gameJsRenderSection", "class Renderer {}"));

        // when
        orchestrator.startRevisionPipeline(project.getId(), "버튼이 안 눌려요");

        // then
        AgentContext ctoCtx = ctoDebugCaptor.getValue();
        assertThat(ctoCtx.getBugReport())
                .as("""
                    startRevisionPipeline QA FAIL 시 CTO debug bugReport가 비어 있음.
                    원인: QA NEEDS_REVIEW 처리 전에 qaResult.getArtifacts()가 artifacts에 putAll되지 않음.
                    """)
                .isNotBlank()
                .contains("FAIL");
    }

    // =========================================================================
    // 시나리오 5: debug loop — fix 후 gameJsLogicSection/gameJsRenderSection 재병합 검증
    // =========================================================================

    @Test
    @DisplayName("[시나리오 5] debug loop: fix 후 gameJsLogicSection+gameJsRenderSection이 CodeMerger에 전달되어야 한다")
    void debugLoop_fixedSections_passedToCodeMerger() {
        // given
        setupNormalPipeline();
        when(stageExecutor.executeAgent(eq(qaAgent), any(), any()))
                .thenReturn(AgentResult.needsReview(
                        Map.of("test-report.json", "{\"result\":\"FAIL\"}"), "fail", 0, 0));
        when(ctoAgent.executeDebug(any()))
                .thenReturn(success("debug-instruction.json", "{}"));

        String fixedLogic  = "class Game { /* fixed */ }";
        String fixedRender = "class Renderer { /* fixed */ }";
        when(backendAgent.executeFix(any()))
                .thenReturn(success("gameJsLogicSection", fixedLogic));
        when(frontendAgent.executeFix(any()))
                .thenReturn(AgentResult.success(
                        Map.of("gameJsRenderSection", fixedRender,
                               "index.html", "<html>", "style.css", "body{}"),
                        "", 0, 0));

        // QA retest → PASS
        when(qaAgent.execute(any()))
                .thenReturn(success("test-report.json", "{\"result\":\"PASS\"}"));

        // CodeMerger 호출 내용 캡처 (병합 시 어떤 값이 들어오는지 확인)
        ArgumentCaptor<String> mergeLogicCaptor  = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> mergeRenderCaptor = ArgumentCaptor.forClass(String.class);
        when(codeMerger.mergeGameJs(mergeLogicCaptor.capture(), mergeRenderCaptor.capture(), any()))
                .thenReturn(VALID_GAME_JS);

        // when
        orchestrator.startPipeline(project.getId());

        // then: debug loop에서 fix 결과로 재병합되어야 함
        // mergeGameJs가 여러 번 호출됨 (초기 병합 + debug 재병합)
        // 마지막 호출이 fix된 값으로 이루어져야 함
        List<String> capturedLogics  = mergeLogicCaptor.getAllValues();
        List<String> capturedRenders = mergeRenderCaptor.getAllValues();

        assertThat(capturedLogics)
                .as("debug loop 재병합 시 BackendAgent.executeFix()가 반환한 gameJsLogicSection이 사용되어야 함")
                .contains(fixedLogic);
        assertThat(capturedRenders)
                .as("debug loop 재병합 시 FrontendAgent.executeFix()가 반환한 gameJsRenderSection이 사용되어야 함")
                .contains(fixedRender);
    }

    // =========================================================================
    // 헬퍼
    // =========================================================================

    /** 정상 파이프라인 스텁 설정 (시나리오 1·2·5 공통).
     *  lenient()를 사용하여 개별 테스트에서 ArgumentCaptor로 덮어써도 오류 없이 통과. */
    private void setupNormalPipeline() {
        lenient().when(stageExecutor.executeAgent(eq(planningAgent), any(), any()))
                .thenReturn(success("plan.json", "{\"title\":\"test\"}"));
        lenient().when(contentAgent.execute(any()))
                .thenReturn(success("content.json", "단어장 데이터"));
        lenient().when(stageExecutor.executeAgent(eq(ctoAgent), any(), any()))
                .thenReturn(success("architecture.json", "{\"arch\":true}"));
        lenient().when(stageExecutor.executeAgent(eq(designAgent), any(), any()))
                .thenReturn(success("design.json", "{\"design\":true}"));
        lenient().when(stageExecutor.executeAgent(eq(backendAgent), any(), any()))
                .thenReturn(success("gameJsLogicSection", "class Game {}"));
        lenient().when(stageExecutor.executeAgent(eq(frontendAgent), any(), any()))
                .thenReturn(AgentResult.success(
                        Map.of("gameJsRenderSection", "class Renderer {}",
                               "index.html", "<html>", "style.css", "body{}"),
                        "", 0, 0));
        lenient().when(stageExecutor.executeAgent(eq(qaAgent), any(), any()))
                .thenReturn(success("test-report.json", "{\"result\":\"PASS\"}"));
    }

    /** startRevisionPipeline 기본 스텁 설정 */
    private void setupRevisionPipelineBase() {
        // loadExistingArtifacts 파일 로드 — readArtifact/readGameFile은 lenient
        lenient().when(fileStorageService.readArtifact(any(), eq("plan.json")))
                .thenReturn("{\"title\":\"test\"}");
        lenient().when(fileStorageService.readArtifact(any(), eq("content.json")))
                .thenReturn("content");
        lenient().when(fileStorageService.readArtifact(any(), eq("architecture.json")))
                .thenReturn("{\"arch\":true}");
        lenient().when(fileStorageService.readArtifact(any(), eq("design.json")))
                .thenReturn("{\"design\":true}");
        lenient().when(fileStorageService.readGameFile(any(), eq("game.js")))
                .thenReturn(VALID_GAME_JS);
        lenient().when(fileStorageService.readGameFile(any(), eq("index.html")))
                .thenReturn("<html>");
        lenient().when(fileStorageService.readGameFile(any(), eq("style.css")))
                .thenReturn("body{}");

        // CTO feedback
        lenient().when(ctoAgent.executeFeedback(any()))
                .thenReturn(success("debug-instruction.json", "{\"instruction\":\"fix\"}"));
        // Fix agents 기본 — lenient (개별 테스트에서 덮어쓸 수 있음)
        lenient().when(frontendAgent.executeFix(any()))
                .thenReturn(AgentResult.success(
                        Map.of("gameJsRenderSection", "class Renderer {}",
                               "index.html", "<html>", "style.css", "body{}"),
                        "", 0, 0));
        lenient().when(backendAgent.executeFix(any()))
                .thenReturn(success("gameJsLogicSection", "class Game {}"));
    }

    private AgentResult success(String key, String value) {
        return AgentResult.success(Map.of(key, value), "", 0, 0);
    }

    private List<Stage> createTestStages(Project project) {
        return List.of(
                Stage.create(project, StageType.PLANNING,       "PLANNING", 1),
                Stage.create(project, StageType.ARCHITECTURE,   "CTO",      2),
                Stage.create(project, StageType.DESIGN,         "DESIGN",   3),
                Stage.create(project, StageType.IMPLEMENTATION, "BACKEND",  4),
                Stage.create(project, StageType.QA,             "QA",       5),
                Stage.create(project, StageType.DEBUG,          "CTO",      6),
                Stage.create(project, StageType.RELEASE,        "SYSTEM",   7)
        );
    }
}
