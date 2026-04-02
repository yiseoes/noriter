package com.noriter.infrastructure.storage;

import com.noriter.infrastructure.storage.FileStorageService.GameFileInfo;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private Path testWorkspace;

    @BeforeEach
    void setUp() throws IOException {
        testWorkspace = Files.createTempDirectory("noriter-test-workspace");
        fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "workspacePath", testWorkspace.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(testWorkspace)) {
            Files.walk(testWorkspace)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        }
    }

    @Test
    @DisplayName("산출물 파일을 저장하고 읽을 수 있다")
    void saveAndReadArtifact() {
        String content = "{\"gameName\":\"테스트 게임\"}";

        String path = fileStorageService.saveArtifact("prj_test", "plan.json", content);

        assertThat(path).isEqualTo("prj_test/artifacts/plan.json");

        String readContent = fileStorageService.readArtifact("prj_test", "plan.json");
        assertThat(readContent).isEqualTo(content);
    }

    @Test
    @DisplayName("게임 파일을 저장하고 읽을 수 있다")
    void saveAndReadGameFile() {
        String html = "<!DOCTYPE html><html><body>게임</body></html>";

        fileStorageService.saveGameFile("prj_test", "index.html", html);

        String readContent = fileStorageService.readGameFile("prj_test", "index.html");
        assertThat(readContent).isEqualTo(html);
    }

    @Test
    @DisplayName("존재하지 않는 파일을 읽으면 null을 반환한다")
    void readNonExistent_returnsNull() {
        String result = fileStorageService.readGameFile("prj_none", "index.html");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("게임 파일 목록을 조회할 수 있다")
    void getGameFiles_returnsFileList() {
        fileStorageService.saveGameFile("prj_test", "index.html", "<html></html>");
        fileStorageService.saveGameFile("prj_test", "style.css", "body{}");
        fileStorageService.saveGameFile("prj_test", "game.js", "class Game{}");

        List<GameFileInfo> files = fileStorageService.getGameFiles("prj_test");

        assertThat(files).hasSize(3);
        assertThat(files).extracting(GameFileInfo::type)
                .containsExactlyInAnyOrder("html", "css", "javascript");
    }

    @Test
    @DisplayName("게임 파일이 없는 프로젝트는 빈 목록을 반환한다")
    void getGameFiles_noFiles_returnsEmpty() {
        List<GameFileInfo> files = fileStorageService.getGameFiles("prj_none");
        assertThat(files).isEmpty();
    }

    @Test
    @DisplayName("게임 파일 존재 여부를 확인할 수 있다")
    void gameFilesExist_checksCorrectly() {
        assertThat(fileStorageService.gameFilesExist("prj_none")).isFalse();

        fileStorageService.saveGameFile("prj_test", "index.html", "<html></html>");
        assertThat(fileStorageService.gameFilesExist("prj_test")).isTrue();
    }

    @Test
    @DisplayName("미리보기 HTML을 반환할 수 있다")
    void getPreviewHtml_returnsIndexHtml() {
        String html = "<!DOCTYPE html><html><body>게임 미리보기</body></html>";
        fileStorageService.saveGameFile("prj_test", "index.html", html);

        String preview = fileStorageService.getPreviewHtml("prj_test");

        assertThat(preview).isEqualTo(html);
    }

    @Test
    @DisplayName("게임 파일이 없으면 미리보기가 null이다")
    void getPreviewHtml_noFiles_returnsNull() {
        String preview = fileStorageService.getPreviewHtml("prj_none");
        assertThat(preview).isNull();
    }

    @Test
    @DisplayName("게임 파일을 ZIP으로 패키징할 수 있다")
    void packageAsZip_createsZip() {
        fileStorageService.saveGameFile("prj_test", "index.html", "<html></html>");
        fileStorageService.saveGameFile("prj_test", "style.css", "body{}");
        fileStorageService.saveGameFile("prj_test", "game.js", "class Game{}");

        byte[] zipBytes = fileStorageService.packageAsZip("prj_test");

        assertThat(zipBytes).isNotNull();
        assertThat(zipBytes.length).isGreaterThan(0);
        // ZIP 매직 넘버: PK (0x504B)
        assertThat(zipBytes[0]).isEqualTo((byte) 0x50);
        assertThat(zipBytes[1]).isEqualTo((byte) 0x4B);
    }

    @Test
    @DisplayName("게임 파일이 없으면 ZIP이 null이다")
    void packageAsZip_noFiles_returnsNull() {
        byte[] zipBytes = fileStorageService.packageAsZip("prj_none");
        assertThat(zipBytes).isNull();
    }

    @Test
    @DisplayName("워크스페이스를 삭제할 수 있다")
    void deleteWorkspace_removesAllFiles() {
        fileStorageService.saveGameFile("prj_test", "index.html", "<html></html>");
        fileStorageService.saveArtifact("prj_test", "plan.json", "{}");

        assertThat(fileStorageService.gameFilesExist("prj_test")).isTrue();

        fileStorageService.deleteWorkspace("prj_test");

        assertThat(fileStorageService.gameFilesExist("prj_test")).isFalse();
        assertThat(fileStorageService.readArtifact("prj_test", "plan.json")).isNull();
    }
}
