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
}
