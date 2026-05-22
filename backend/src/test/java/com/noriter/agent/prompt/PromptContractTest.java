package com.noriter.agent.prompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 에이전트 프롬프트 계약 정합성 테스트
 *
 * [검사 1] 프롬프트 템플릿의 {{변수명}}이 Java Map.of()에 모두 전달되는지
 *   - 미전달 시: PromptTemplate이 {{var}} 그대로 AI에게 넘김 → AI가 리터럴 문자열을 컨텍스트로 오해
 *
 * [검사 2] CodeMerger DEFAULT_INIT_CODE의 renderer.xxx() 호출이 CTO publicMethods에 선언되었는지
 *   - 미선언 시: Frontend가 해당 메서드 구현 안 함 → 런타임 "is not a function" 오류
 *
 * [검사 3] 에이전트 간 artifact 키가 생산자↔소비자 간 일치하는지
 *   - 불일치 시: 소비자가 빈 문자열을 받아 컨텍스트 없이 코드 생성
 */
@DisplayName("프롬프트 계약 정합성")
class PromptContractTest {

    private static final Path TEMPLATES_DIR =
            Paths.get("src/main/resources/templates");
    private static final Path AGENT_IMPL_DIR =
            Paths.get("src/main/java/com/noriter/agent/impl");
    private static final Path PIPELINE_DIR =
            Paths.get("src/main/java/com/noriter/agent/pipeline");
    private static final Path ORCHESTRATOR_PATH =
            PIPELINE_DIR.resolve("PipelineOrchestrator.java");

    private static final Pattern TEMPLATE_VAR = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    // =========================================================================
    // 검사 1: {{변수명}} ↔ Java Map.of() 계약
    // =========================================================================

    record PromptContract(
            String description,
            String templateFile,
            Set<String> javaMapKeys   // Java 코드의 Map.of()에 실제로 전달하는 키 목록
    ) {}

    static Stream<PromptContract> promptContracts() {
        return Stream.of(
            new PromptContract(
                "PlanningAgent.execute() → plan-main-user.txt",
                "plan-main-user.txt",
                Set.of("requirement", "genre")
            ),
            new PromptContract(
                "ContentAgent.execute() → content-main-user.txt",
                "content-main-user.txt",
                Set.of("plan", "requirement")
            ),
            new PromptContract(
                "CtoAgent.execute() → cto-main-user.txt",
                "cto-main-user.txt",
                Set.of("plan", "contentData")
            ),
            new PromptContract(
                "CtoAgent.executeFeedback() → cto-feedback-user.txt",
                "cto-feedback-user.txt",
                Set.of("feedback", "plan", "architecture", "indexHtml", "styleCss", "gameJs", "renderCode")
            ),
            new PromptContract(
                "CtoAgent.executeDebug() → cto-debug-user.txt",
                "cto-debug-user.txt",
                Set.of("plan", "bugReport", "architecture", "gameJs", "indexHtml", "styleCss", "renderCode")
            ),
            new PromptContract(
                "DesignAgent.execute() → design-main-user.txt",
                "design-main-user.txt",
                Set.of("plan", "architecture", "contentData")
            ),
            new PromptContract(
                "BackendAgent.execute() → back-main-user.txt",
                "back-main-user.txt",
                Set.of("plan", "architecture", "design", "contentData")
            ),
            new PromptContract(
                "BackendAgent.executeFix() → back-fix-user.txt",
                "back-fix-user.txt",
                Set.of("ctoInstruction", "bugReport", "gameJs")
            ),
            new PromptContract(
                "FrontendAgent.execute() → front-main-user.txt",
                "front-main-user.txt",
                Set.of("plan", "architecture", "design", "backendCode")
            ),
            new PromptContract(
                "FrontendAgent.executeFix() → front-fix-user.txt",
                "front-fix-user.txt",
                Set.of("bugReport", "ctoInstruction", "indexHtml", "styleCss", "renderCode")
            ),
            new PromptContract(
                "QaAgent.execute() 초기 → qa-main-user.txt",
                "qa-main-user.txt",
                Set.of("plan", "architecture", "indexHtml", "styleCss", "gameJs")
            ),
            new PromptContract(
                "QaAgent.execute() 재검증 → qa-retest-user.txt",
                "qa-retest-user.txt",
                Set.of("plan", "previousReport", "architecture", "indexHtml", "styleCss", "gameJs")
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("promptContracts")
    @DisplayName("[검사 1] 프롬프트 {{변수}}가 Java Map에 선언된 키와 완전히 일치해야 한다")
    void templateVars_mustMatchJavaMapKeys(PromptContract contract) throws IOException {
        String templateContent = readTemplate(contract.templateFile());
        Set<String> templateVars = extractTemplateVars(templateContent);

        // 템플릿에 있는데 Java Map에 없음 → AI에게 {{var}} 리터럴이 그대로 전달됨
        Set<String> missingInJava = new HashSet<>(templateVars);
        missingInJava.removeAll(contract.javaMapKeys());

        // Java Map에 있는데 템플릿에 없음 → 불필요한 토큰 낭비 (경고 수준)
        Set<String> unusedInTemplate = new HashSet<>(contract.javaMapKeys());
        unusedInTemplate.removeAll(templateVars);

        assertThat(missingInJava)
                .as("""
                    [%s]
                    템플릿에 {{%s}} 있는데 Java Map.of()에 없음.
                    → AI가 리터럴 {{%s}} 문자열을 컨텍스트로 받아 엉뚱한 코드 생성.
                    → Java 에이전트 코드의 Map.of()에 해당 키를 추가하세요.
                    """,
                        contract.description(),
                        String.join("}}, {{", missingInJava),
                        String.join("}}, {{", missingInJava))
                .isEmpty();

        assertThat(unusedInTemplate)
                .as("""
                    [%s]
                    Java Map.of()에 '%s' 전달하지만 템플릿에서 사용하지 않음.
                    → 불필요한 토큰 낭비. 템플릿에 {{%s}} 추가하거나 Java Map에서 제거하세요.
                    """,
                        contract.description(),
                        String.join(", ", unusedInTemplate),
                        String.join("}}, {{", unusedInTemplate))
                .isEmpty();
    }

    // =========================================================================
    // 검사 2: CodeMerger renderer.xxx() 호출 ↔ CTO publicMethods 선언
    // =========================================================================

    @Test
    @DisplayName("[검사 2] CodeMerger DEFAULT_INIT_CODE의 renderer.xxx() 호출이 CTO publicMethods에 모두 선언되어야 한다")
    void codeMerger_rendererCalls_mustBeDeclaredInCtoPublicMethods() throws IOException {
        String codeMergerSource = Files.readString(PIPELINE_DIR.resolve("CodeMerger.java"));
        String ctoSystemPrompt = readTemplate("cto-main-system.txt");

        // CodeMerger.java 에서 renderer.xxx( 패턴 추출
        Pattern rendererCall = Pattern.compile("renderer\\.([a-zA-Z]+)\\s*\\(");
        Set<String> calledMethods = new LinkedHashSet<>();
        Matcher m1 = rendererCall.matcher(codeMergerSource);
        while (m1.find()) {
            calledMethods.add(m1.group(1));
        }

        // cto-main-system.txt 에서 "name": "methodName" 패턴 추출
        Pattern methodName = Pattern.compile("\"name\"\\s*:\\s*\"([a-zA-Z]+)\"");
        Set<String> declaredMethods = new LinkedHashSet<>();
        Matcher m2 = methodName.matcher(ctoSystemPrompt);
        while (m2.find()) {
            declaredMethods.add(m2.group(1));
        }

        Set<String> undeclared = new HashSet<>(calledMethods);
        undeclared.removeAll(declaredMethods);

        assertThat(undeclared)
                .as("""
                    CodeMerger DEFAULT_INIT_CODE에서 renderer.%s() 호출하지만
                    cto-main-system.txt rendererClass.publicMethods에 선언 없음.
                    → Frontend(Renderer)가 해당 메서드를 구현하지 않을 수 있음.
                    → cto-main-system.txt publicMethods에 {"name": "%s", ...} 항목을 추가하세요.
                    """,
                        String.join("(), renderer.", undeclared),
                        String.join("\", ...}, {\"name\": \"", undeclared))
                .isEmpty();
    }

    // =========================================================================
    // 검사 3: 에이전트 간 artifact 키 생산자↔소비자 일치
    // =========================================================================

    record ArtifactFlow(String producerFile, String artifactKey, String consumerFile, String description) {}

    static Stream<ArtifactFlow> artifactFlows() {
        return Stream.of(
            // PlanningAgent → 하위 에이전트들
            new ArtifactFlow("PlanningAgent.java", "plan.json", "ContentAgent.java",   "PlanningAgent → ContentAgent"),
            new ArtifactFlow("PlanningAgent.java", "plan.json", "CtoAgent.java",       "PlanningAgent → CtoAgent"),
            new ArtifactFlow("PlanningAgent.java", "plan.json", "DesignAgent.java",    "PlanningAgent → DesignAgent"),
            new ArtifactFlow("PlanningAgent.java", "plan.json", "BackendAgent.java",   "PlanningAgent → BackendAgent"),
            new ArtifactFlow("PlanningAgent.java", "plan.json", "FrontendAgent.java",  "PlanningAgent → FrontendAgent"),
            new ArtifactFlow("PlanningAgent.java", "plan.json", "QaAgent.java",        "PlanningAgent → QaAgent"),
            // ContentAgent → CTO / Design / Backend
            new ArtifactFlow("ContentAgent.java",  "content.json", "CtoAgent.java",    "ContentAgent → CtoAgent"),
            new ArtifactFlow("ContentAgent.java",  "content.json", "DesignAgent.java", "ContentAgent → DesignAgent"),
            new ArtifactFlow("ContentAgent.java",  "content.json", "BackendAgent.java","ContentAgent → BackendAgent"),
            // CtoAgent → Design / Backend / Frontend / QA
            new ArtifactFlow("CtoAgent.java",      "architecture.json", "DesignAgent.java",   "CtoAgent → DesignAgent"),
            new ArtifactFlow("CtoAgent.java",      "architecture.json", "BackendAgent.java",  "CtoAgent → BackendAgent"),
            new ArtifactFlow("CtoAgent.java",      "architecture.json", "FrontendAgent.java", "CtoAgent → FrontendAgent"),
            new ArtifactFlow("CtoAgent.java",      "architecture.json", "QaAgent.java",       "CtoAgent → QaAgent"),
            // DesignAgent → Backend / Frontend
            new ArtifactFlow("DesignAgent.java",   "design.json", "BackendAgent.java",  "DesignAgent → BackendAgent"),
            new ArtifactFlow("DesignAgent.java",   "design.json", "FrontendAgent.java", "DesignAgent → FrontendAgent"),
            // BackendAgent → FrontendAgent (Game 클래스 섹션)
            new ArtifactFlow("BackendAgent.java",  "gameJsLogicSection", "FrontendAgent.java", "BackendAgent → FrontendAgent (Game 클래스)")
        );
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("artifactFlows")
    @DisplayName("[검사 3] artifact 키가 생산자와 소비자 양쪽에 모두 존재해야 한다")
    void artifactKeys_mustExistInBothProducerAndConsumer(ArtifactFlow flow) throws IOException {
        String producerSource = Files.readString(AGENT_IMPL_DIR.resolve(flow.producerFile()));
        String consumerSource = Files.readString(AGENT_IMPL_DIR.resolve(flow.consumerFile()));

        assertThat(producerSource)
                .as("""
                    [생산자 %s] artifact 키 "%s"를 생성하지 않습니다.
                    → Map.of() 또는 artifacts.put()에 해당 키가 없음.
                    """, flow.producerFile(), flow.artifactKey())
                .contains("\"" + flow.artifactKey() + "\"");

        assertThat(consumerSource)
                .as("""
                    [소비자 %s] artifact 키 "%s"를 읽지 않습니다.
                    → getOrDefault() 또는 containsKey()에 해당 키가 없음.
                    → 해당 에이전트가 빈 문자열을 컨텍스트로 받아 잘못된 코드 생성 가능.
                    """, flow.consumerFile(), flow.artifactKey())
                .contains("\"" + flow.artifactKey() + "\"");
    }

    // =========================================================================
    // 검사 4: PipelineOrchestrator — resume/revision 파이프라인 artifact 흐름
    //
    // 이 그룹은 Java 코드 패턴을 정적 분석한다.
    // 아래 두 버그 유형을 자동으로 감지한다:
    //   (A) loadExistingArtifacts()에 필요한 키가 빠져 있는 경우
    //       → resumePipeline 재시작 시 에이전트가 빈 문자열 컨텍스트로 실행됨
    //   (B) QA NEEDS_REVIEW 처리 전에 qaResult.getArtifacts()를 저장하지 않는 경우
    //       → handleQaFailure의 CTO debug context에 test-report.json = "" 전달됨
    // =========================================================================

    /**
     * resume/revision 파이프라인에서 필요한 모든 artifact를
     * loadExistingArtifacts()가 로드하는지 전수 검사.
     *
     * 미포함 시: resumePipeline(fromStage >= ARCHITECTURE)에서
     * 해당 artifact를 빈 문자열로 에이전트에 전달.
     */
    @ParameterizedTest(name = "\"{0}\" 가 loadExistingArtifacts()에 있어야 한다")
    @MethodSource("resumeRequiredArtifacts")
    @DisplayName("[검사 4-1] loadExistingArtifacts()가 resume 시 필요한 모든 artifact를 로드해야 한다")
    void loadExistingArtifacts_mustLoadAllResumeArtifacts(String artifactKey) throws IOException {
        String code = Files.readString(ORCHESTRATOR_PATH);
        String methodBody = extractMethodBody(code, "loadExistingArtifacts");

        assertThat(methodBody)
                .as("""
                    loadExistingArtifacts()에 "%s" 없음.
                    → resumePipeline 재시작 시 해당 artifact를 빈 문자열로 에이전트에 전달.
                    → artifactFiles (또는 gameFiles) 배열에 "%s" 추가 필요.
                    """, artifactKey, artifactKey)
                .contains("\"" + artifactKey + "\"");
    }

    static Stream<String> resumeRequiredArtifacts() {
        // 각 항목은 이 artifact를 사용하는 에이전트와 함께 명시
        // plan.json        → ContentAgent, CtoAgent, DesignAgent, BackendAgent, FrontendAgent, QaAgent
        // content.json     → BackendAgent.execute()  (단어장/스테이지 데이터)
        // architecture.json → DesignAgent, BackendAgent, FrontendAgent, QaAgent
        // design.json      → BackendAgent, FrontendAgent
        // game.js          → QaAgent, JsSyntaxValidator, JsRuntimeValidator
        // index.html, style.css → QaAgent
        return Stream.of(
                "plan.json",
                "content.json",
                "architecture.json",
                "design.json",
                "game.js",
                "index.html",
                "style.css"
        );
    }

    /**
     * QA가 NEEDS_REVIEW를 반환할 때 handleQaFailure() 호출 전에
     * qaResult.getArtifacts()를 artifacts map에 저장하는지 검사.
     *
     * 미저장 시:
     * - handleQaFailure 내 CTO debug context: bugReport = "" → 수정 지시 없이 재시도
     * - QA retest context: previousReport = "" → 이전 버그 내용 없이 재검증
     *
     * startPipeline / resumePipeline / startRevisionPipeline 3개 경로 모두 검사.
     */
    @Test
    @DisplayName("[검사 4-2] handleQaFailure() 호출 전에 qaResult.getArtifacts()가 artifacts에 저장되어야 한다")
    void qaArtifacts_mustBeSavedBeforeHandleQaFailure() throws IOException {
        String code = Files.readString(ORCHESTRATOR_PATH);

        int searchFrom = 0;
        int handleQaCount = 0;

        // "handleQaFailure(project," — 실제 호출 패턴
        // 메서드 정의 "private void handleQaFailure(Project project," 는 타입명이 앞에 있어 제외됨
        final String CALL_PATTERN = "handleQaFailure(project,";
        while (true) {
            int handleIdx = code.indexOf(CALL_PATTERN, searchFrom);
            if (handleIdx < 0) break;
            handleQaCount++;

            // handleQaFailure 호출 앞 400자 내에 putAll이 있어야 함
            int lookbackStart = Math.max(0, handleIdx - 400);
            String lookback = code.substring(lookbackStart, handleIdx);

            assertThat(lookback)
                    .as("""
                        handleQaFailure(project, ...) 호출(코드 위치 index=%d) 전에
                        artifacts.putAll(qaResult.getArtifacts()) 없음.
                        → CTO debug context: bugReport="" (test-report.json 미전달)
                        → QA retest: previousReport="" (이전 버그 내용 없음)
                        → handleQaFailure 호출 직전에 다음 추가:
                          if (qaResult.getArtifacts() != null) artifacts.putAll(qaResult.getArtifacts());
                        """, handleIdx)
                    .contains("putAll");

            searchFrom = handleIdx + 1;
        }

        assertThat(handleQaCount)
                .as("handleQaFailure(project, ...) 호출이 1회도 없음 — 파이프라인 구조 변경 확인 필요")
                .isGreaterThanOrEqualTo(1);
    }

    // =========================================================================
    // 헬퍼
    // =========================================================================

    private String readTemplate(String filename) throws IOException {
        return Files.readString(TEMPLATES_DIR.resolve(filename));
    }

    private Set<String> extractTemplateVars(String content) {
        Set<String> vars = new LinkedHashSet<>();
        Matcher m = TEMPLATE_VAR.matcher(content);
        while (m.find()) {
            vars.add(m.group(1));
        }
        return vars;
    }

    /**
     * 소스코드에서 특정 메서드의 본문을 추출한다.
     *
     * 두 가지 문제를 방지:
     * (1) call vs definition 구분 — access modifier(private/protected/public)로 정의만 탐색
     * (2) 문자열 리터럴 내 {} 무시 — log.info("...{}...") 패턴에서 오탐 방지
     */
    private String extractMethodBody(String source, String methodName) {
        // 메서드 정의만 매칭: (private|protected|public) [반환타입] methodName(
        // [^({]+ : 괄호·중괄호 없는 반환 타입 (greedy → backtrack으로 methodName 앞에 멈춤)
        Pattern defPattern = Pattern.compile(
                "(?:private|protected|public)\\s[^({]+\\b" + Pattern.quote(methodName) + "\\s*\\("
        );
        Matcher m = defPattern.matcher(source);
        if (!m.find()) return "";
        int sigIdx = m.start();

        // m.end() = "methodName(" 이후. 문자열 리터럴 내 {}를 무시하며 메서드 본문 끝 탐색.
        boolean inString = false;
        int depth = 0;
        for (int i = m.end(); i < source.length(); i++) {
            char c = source.charAt(i);
            if (inString) {
                if (c == '\\') { i++; continue; }  // 이스케이프 다음 문자 건너뜀
                if (c == '"')  { inString = false; }
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == '{')      depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return source.substring(sigIdx, i + 1);
            }
        }
        return source.substring(sigIdx);
    }
}
