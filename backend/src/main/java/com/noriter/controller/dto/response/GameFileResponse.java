package com.noriter.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * API-GAM-002: 게임 소스 코드 목록 응답
 * 참조: 05_API §4 API-GAM-002 응답
 */
@Getter
@AllArgsConstructor
public class GameFileResponse {

    private final List<FileInfo> files;

    @Getter
    @AllArgsConstructor
    public static class FileInfo {
        private final String path;
        private final long size;
        private final String type;
    }
}
