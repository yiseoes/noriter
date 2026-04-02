package com.noriter.controller;

import com.noriter.controller.dto.response.GameFileResponse;
import com.noriter.exception.ErrorCode;
import com.noriter.exception.NoriterException;
import com.noriter.infrastructure.storage.FileStorageService;
import com.noriter.infrastructure.storage.FileStorageService.GameFileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 게임 미리보기 및 다운로드 API
 * 참조: 05_API §3 API-GAM-001~004 (4개 엔드포인트)
 */
@Log4j2
@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class GameController {

    private final FileStorageService fileStorageService;

    /**
     * API-GAM-001: 게임 미리보기
     * GET /api/projects/{id}/preview
     * 응답: text/html
     */
    @GetMapping("/preview")
    public ResponseEntity<String> getPreview(@PathVariable String id) {
        log.info("[API-GAM-001] 게임 미리보기 요청 - projectId={}", id);

        String html = fileStorageService.getPreviewHtml(id);
        if (html == null) {
            log.warn("[API-GAM-001] 게임 파일 없음 - projectId={}", id);
            throw new NoriterException(ErrorCode.GAME_NOT_FOUND);
        }

        log.info("[API-GAM-001] 미리보기 응답 완료 - projectId={}, 크기={}bytes", id, html.length());
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    /**
     * API-GAM-002: 게임 소스 코드 목록
     * GET /api/projects/{id}/source
     */
    @GetMapping("/source")
    public ResponseEntity<GameFileResponse> getSourceFiles(@PathVariable String id) {
        log.info("[API-GAM-002] 소스 코드 목록 조회 - projectId={}", id);

        List<GameFileInfo> files = fileStorageService.getGameFiles(id);
        List<GameFileResponse.FileInfo> fileInfos = files.stream()
                .map(f -> new GameFileResponse.FileInfo(f.path(), f.size(), f.type()))
                .toList();

        log.info("[API-GAM-002] 소스 목록 응답 - projectId={}, {}개 파일", id, fileInfos.size());
        return ResponseEntity.ok(new GameFileResponse(fileInfos));
    }

    /**
     * API-GAM-003: 소스 파일 내용 조회
     * GET /api/projects/{id}/source/{path}
     * 응답: text/plain
     */
    @GetMapping("/source/{path}")
    public ResponseEntity<String> getSourceFile(@PathVariable String id, @PathVariable String path) {
        log.info("[API-GAM-003] 소스 파일 조회 - projectId={}, path={}", id, path);

        String content = fileStorageService.readGameFile(id, path);
        if (content == null) {
            log.warn("[API-GAM-003] 파일 없음 - projectId={}, path={}", id, path);
            throw new NoriterException(ErrorCode.GAME_NOT_FOUND);
        }

        log.info("[API-GAM-003] 소스 파일 응답 - projectId={}, path={}, 크기={}bytes",
                id, path, content.length());
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(content);
    }

    /**
     * API-GAM-004: 게임 ZIP 다운로드
     * GET /api/projects/{id}/download
     * 응답: application/zip
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadGame(@PathVariable String id) {
        log.info("[API-GAM-004] 게임 다운로드 요청 - projectId={}", id);

        byte[] zipBytes = fileStorageService.packageAsZip(id);
        if (zipBytes == null) {
            log.warn("[API-GAM-004] 게임 파일 없음, 다운로드 불가 - projectId={}", id);
            throw new NoriterException(ErrorCode.GAME_NOT_FOUND);
        }

        log.info("[API-GAM-004] ZIP 다운로드 응답 - projectId={}, 크기={}bytes", id, zipBytes.length);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("game_" + id + ".zip")
                .build());

        return ResponseEntity.ok().headers(headers).body(zipBytes);
    }
}
