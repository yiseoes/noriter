package com.noriter.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Node.js를 이용한 game.js 런타임 검증기
 *
 * 브라우저 API를 모킹한 환경에서 game.js를 실제로 로드하고
 * Game/Renderer 인스턴스화까지 실행해 런타임 오류를 잡는다.
 * Claude API 비용 없음. 몇 초 안에 완료.
 */
@Log4j2
public class JsRuntimeValidator {

    private static final int TIMEOUT_SECONDS = 15;

    public record ValidationResult(boolean valid, String errorType, String errorMessage) {
        public static ValidationResult pass() {
            return new ValidationResult(true, null, null);
        }
        public static ValidationResult fail(String errorType, String errorMessage) {
            return new ValidationResult(false, errorType, errorMessage);
        }
        public String summary() {
            if (valid) return "PASS";
            return errorType + ": " + errorMessage;
        }
    }

    /**
     * game.js 코드를 실제로 Node.js로 실행해 검증한다.
     */
    public static ValidationResult validate(String gameJsContent) {
        if (gameJsContent == null || gameJsContent.isBlank()) {
            return ValidationResult.fail("EMPTY", "game.js가 비어 있습니다");
        }

        Path tempGameJs = null;
        Path validatorScript = null;
        boolean validatorIsTempFile = false;

        try {
            // game.js를 임시 파일로 저장
            tempGameJs = Files.createTempFile("noriter-game-", ".js");
            Files.writeString(tempGameJs, gameJsContent, StandardCharsets.UTF_8);

            // game-validator.js 경로 확인 (resources에서 추출)
            try {
                ClassPathResource resource = new ClassPathResource("game-validator.js");
                validatorScript = resource.getFile().toPath();
            } catch (Exception e) {
                // IDE 환경 등에서 classpath에서 직접 파일 못 찾을 경우 임시 파일 생성
                log.debug("[런타임검증] classpath 로드 실패, 임시 파일로 추출: {}", e.getMessage());
                ClassPathResource resource = new ClassPathResource("game-validator.js");
                validatorScript = Files.createTempFile("game-validator-", ".js");
                Files.copy(resource.getInputStream(), validatorScript, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                validatorIsTempFile = true;
            }

            // Node.js 실행
            ProcessBuilder pb = new ProcessBuilder(
                    "node", validatorScript.toAbsolutePath().toString(),
                    tempGameJs.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // stdout / stderr 수집
            List<String> stdoutLines = new ArrayList<>();
            List<String> stderrLines = new ArrayList<>();

            Thread stdoutReader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) stdoutLines.add(line);
                } catch (Exception ignored) {}
            });
            Thread stderrReader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) stderrLines.add(line);
                } catch (Exception ignored) {}
            });
            stdoutReader.start();
            stderrReader.start();

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            stdoutReader.join(2000);
            stderrReader.join(2000);

            if (!finished) {
                process.destroyForcibly();
                log.warn("[런타임검증] 타임아웃 ({}초 초과)", TIMEOUT_SECONDS);
                return ValidationResult.fail("TIMEOUT", "Node.js 실행이 " + TIMEOUT_SECONDS + "초를 초과했습니다");
            }

            int exitCode = process.exitValue();
            log.debug("[런타임검증] 종료코드={}, stdout={}, stderr={}", exitCode, stdoutLines, stderrLines);

            if (exitCode == 0) {
                log.info("[런타임검증] PASS");
                return ValidationResult.pass();
            }

            // 오류 파싱
            String errorLine = stderrLines.isEmpty() ? "알 수 없는 오류" : stderrLines.get(0);
            String errorType = parseErrorType(errorLine);
            log.warn("[런타임검증] FAIL - {}: {}", errorType, errorLine);
            return ValidationResult.fail(errorType, errorLine);

        } catch (Exception e) {
            log.error("[런타임검증] 검증 실행 중 예외: {}", e.getMessage(), e);
            return ValidationResult.fail("VALIDATOR_ERROR", e.getMessage());
        } finally {
            // 임시 파일 정리
            if (tempGameJs != null) {
                try { Files.deleteIfExists(tempGameJs); } catch (Exception ignored) {}
            }
            if (validatorIsTempFile && validatorScript != null) {
                try { Files.deleteIfExists(validatorScript); } catch (Exception ignored) {}
            }
        }
    }

    private static String parseErrorType(String errorLine) {
        if (errorLine == null) return "UNKNOWN";
        if (errorLine.startsWith("LOAD_ERROR")) return "LOAD_ERROR";
        if (errorLine.startsWith("RENDERER_ERROR")) return "RENDERER_ERROR";
        if (errorLine.startsWith("GAME_ERROR")) return "GAME_ERROR";
        if (errorLine.startsWith("FILE_ERROR")) return "FILE_ERROR";
        if (errorLine.startsWith("TIMEOUT")) return "TIMEOUT";
        return "RUNTIME_ERROR";
    }
}
