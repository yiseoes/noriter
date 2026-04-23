package com.noriter.infrastructure.storage;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 파일 저장 서비스
 * 참조: 03_아키텍처 §5.3 FileStorage
 *
 * workspace/{projectId}/artifacts/  — 에이전트 산출물 (JSON)
 * workspace/{projectId}/game/       — 게임 소스 코드
 */
@Log4j2
@Service
public class FileStorageService {

    @Value("${noriter.workspace.path:./workspace}")
    private String workspacePath;

    /**
     * 산출물 저장 (JSON 파일)
     * 참조: 03_아키텍처 §5.3 saveArtifact()
     */
    public String saveArtifact(String projectId, String fileName, String content) {
        log.info("[파일 저장] 산출물 저장 시작 - projectId={}, fileName={}, 크기={}bytes",
                projectId, fileName, content.length());

        String relativePath = projectId + "/artifacts/" + fileName;
        Path filePath = resolveWorkspacePath(relativePath);

        writeFile(filePath, content);
        log.info("[파일 저장] 산출물 저장 완료 - path={}", relativePath);
        return relativePath;
    }

    /**
     * 게임 파일 저장
     * 참조: 03_아키텍처 §5.3 saveGameFiles()
     */
    public String saveGameFile(String projectId, String fileName, String content) {
        log.info("[파일 저장] 게임 파일 저장 시작 - projectId={}, fileName={}, 크기={}bytes",
                projectId, fileName, content.length());

        String relativePath = projectId + "/game/" + fileName;
        Path filePath = resolveWorkspacePath(relativePath);

        writeFile(filePath, content);
        log.info("[파일 저장] 게임 파일 저장 완료 - path={}", relativePath);
        return relativePath;
    }

    /**
     * 산출물 읽기
     * 참조: 03_아키텍처 §5.3 getArtifact()
     */
    public String readArtifact(String projectId, String fileName) {
        log.debug("[파일 읽기] 산출물 읽기 - projectId={}, fileName={}", projectId, fileName);

        String relativePath = projectId + "/artifacts/" + fileName;
        Path filePath = resolveWorkspacePath(relativePath);

        return readFile(filePath);
    }

    /**
     * 게임 파일 읽기
     */
    public String readGameFile(String projectId, String fileName) {
        log.debug("[파일 읽기] 게임 파일 읽기 - projectId={}, fileName={}", projectId, fileName);

        String relativePath = projectId + "/game/" + fileName;
        Path filePath = resolveWorkspacePath(relativePath);

        return readFile(filePath);
    }

    /**
     * 게임 파일 목록 조회
     * 참조: 03_아키텍처 §5.3 getGameFiles()
     */
    public List<GameFileInfo> getGameFiles(String projectId) {
        log.debug("[파일 목록] 게임 파일 목록 조회 - projectId={}", projectId);

        Path gameDir = resolveWorkspacePath(projectId + "/game");
        List<GameFileInfo> files = new ArrayList<>();

        if (!Files.exists(gameDir)) {
            log.debug("[파일 목록] 게임 디렉토리 없음 - projectId={}", projectId);
            return files;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(gameDir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    String name = entry.getFileName().toString();
                    long size = Files.size(entry);
                    String type = detectFileType(name);
                    files.add(new GameFileInfo(name, size, type));
                    log.debug("[파일 목록] 파일 발견 - name={}, size={}, type={}", name, size, type);
                }
            }
        } catch (IOException e) {
            log.error("[파일 목록] 디렉토리 읽기 실패 - projectId={}, error={}", projectId, e.getMessage());
        }

        log.debug("[파일 목록] 조회 완료 - projectId={}, {}개 파일", projectId, files.size());
        return files;
    }

    /**
     * 게임 파일 존재 여부 확인
     */
    public boolean gameFilesExist(String projectId) {
        Path gameDir = resolveWorkspacePath(projectId + "/game");
        boolean exists = Files.exists(gameDir) && gameDir.toFile().listFiles() != null
                && gameDir.toFile().listFiles().length > 0;
        log.debug("[파일 확인] 게임 파일 존재 여부 - projectId={}, exists={}", projectId, exists);
        return exists;
    }

    /**
     * 게임 미리보기 HTML 조합
     * 참조: API-GAM-001 — text/html 응답
     */
    public String getPreviewHtml(String projectId) {
        log.info("[미리보기] HTML 조합 시작 - projectId={}", projectId);

        String indexHtml = readGameFile(projectId, "index.html");
        if (indexHtml == null) {
            log.warn("[미리보기] index.html 없음 - projectId={}", projectId);
            return null;
        }

        // 상대경로 → 절대 API 경로 교체 (MIME 타입 오류 방지)
        String base = "/api/projects/" + projectId + "/game/";
        String rewritten = indexHtml
                .replace("href=\"style.css\"",  "href=\"" + base + "style.css\"")
                .replace("href='style.css'",    "href='" + base + "style.css'")
                .replace("src=\"game.js\"",     "src=\"" + base + "game.js\"")
                .replace("src='game.js'",       "src='" + base + "game.js'")
                .replace("src=\"style.css\"",   "src=\"" + base + "style.css\"")
                .replace("src='style.css'",     "src='" + base + "style.css'");

        log.info("[미리보기] HTML 조합 완료 - projectId={}, 크기={}bytes",
                projectId, rewritten.length());
        return rewritten;
    }

    /**
     * ZIP 패키징
     * 참조: 03_아키텍처 §5.3 packageAsZip(), API-GAM-004
     */
    public byte[] packageAsZip(String projectId) {
        log.info("[ZIP 패키징] 시작 - projectId={}", projectId);

        List<GameFileInfo> files = getGameFiles(projectId);
        if (files.isEmpty()) {
            log.warn("[ZIP 패키징] 게임 파일 없음 - projectId={}", projectId);
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (GameFileInfo fileInfo : files) {
                String content = readGameFile(projectId, fileInfo.path());
                if (content != null) {
                    ZipEntry entry = new ZipEntry(fileInfo.path());
                    zos.putNextEntry(entry);
                    zos.write(content.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                    log.debug("[ZIP 패키징] 파일 추가 - {}", fileInfo.path());
                }
            }

            zos.finish();
            byte[] zipBytes = baos.toByteArray();
            log.info("[ZIP 패키징] 완료 - projectId={}, 크기={}bytes, 파일 {}개",
                    projectId, zipBytes.length, files.size());
            return zipBytes;

        } catch (IOException e) {
            log.error("[ZIP 패키징] 실패 - projectId={}, error={}", projectId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 프로젝트 워크스페이스 삭제
     */
    public void deleteWorkspace(String projectId) {
        log.info("[워크스페이스 삭제] 시작 - projectId={}", projectId);

        Path projectDir = resolveWorkspacePath(projectId);
        if (Files.exists(projectDir)) {
            try {
                Files.walk(projectDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("[워크스페이스 삭제] 파일 삭제 실패 - path={}", path);
                            }
                        });
                log.info("[워크스페이스 삭제] 완료 - projectId={}", projectId);
            } catch (IOException e) {
                log.error("[워크스페이스 삭제] 실패 - projectId={}, error={}", projectId, e.getMessage());
            }
        }
    }

    private Path resolveWorkspacePath(String relativePath) {
        return Path.of(workspacePath).resolve(relativePath);
    }

    private void writeFile(Path filePath, String content) {
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("[파일 쓰기] 실패 - path={}, error={}", filePath, e.getMessage(), e);
            throw new RuntimeException("파일 저장 실패: " + filePath, e);
        }
    }

    private String readFile(Path filePath) {
        if (!Files.exists(filePath)) {
            log.debug("[파일 읽기] 파일 없음 - path={}", filePath);
            return null;
        }
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("[파일 읽기] 실패 - path={}, error={}", filePath, e.getMessage());
            return null;
        }
    }

    private String detectFileType(String fileName) {
        if (fileName.endsWith(".html")) return "html";
        if (fileName.endsWith(".css")) return "css";
        if (fileName.endsWith(".js")) return "javascript";
        if (fileName.endsWith(".json")) return "json";
        return "text";
    }

    public record GameFileInfo(String path, long size, String type) {
    }
}
