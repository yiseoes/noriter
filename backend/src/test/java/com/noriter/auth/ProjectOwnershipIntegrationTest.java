package com.noriter.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noriter.domain.Project;
import com.noriter.domain.enums.Genre;
import com.noriter.repository.ProjectRepository;
import com.noriter.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 프로젝트 소유권 격리 통합 테스트
 * 유저 A ↔ 유저 B, 게스트 A ↔ 게스트 B 간 데이터가 섞이지 않는지 검증
 *
 * 삭제/상세 테스트는 비동기 파이프라인 간섭을 피하기 위해
 * Repository로 직접 프로젝트를 생성하여 순수 소유권 로직만 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectOwnershipIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private JwtUtil jwtUtil;

    @BeforeEach
    void cleanUp() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    /** 회원가입하고 토큰 + userId 반환 */
    private String[] signupAndGetTokenAndId(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"pass1234\",\"name\":\"%s\"}", email, name)))
                .andExpect(status().isCreated())
                .andReturn();
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new String[]{ json.get("token").asText(), json.get("userId").asText() };
    }

    /** DB에 직접 프로젝트 생성 (파이프라인 없이, CREATED 상태 = 삭제 가능) */
    private String createProjectDirectly(Long userId, String guestId, String name) {
        Project project = Project.create(name, "테스트 요구사항 열자 이상입니다", Genre.ACTION, 3, true, guestId, userId);
        projectRepository.save(project);
        return project.getId();
    }

    /** API를 통한 데모 프로젝트 생성 (파이프라인 시작됨) */
    private String createDemoProjectViaApi(String token, String guestId, String projectName) throws Exception {
        var builder = post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"name\":\"%s\",\"requirement\":\"테스트용 요구사항 열자 이상입니다\",\"demo\":true}", projectName));

        if (token != null) builder = builder.header("Authorization", "Bearer " + token);
        if (guestId != null) builder = builder.header("X-Guest-Id", guestId);

        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // ============================================================
    // 1. 로그인 유저 간 목록 격리
    // ============================================================

    @Test
    @DisplayName("유저A가 만든 프로젝트는 유저A 목록에만 보인다")
    void loggedInUser_seesOnlyOwnProjects() throws Exception {
        String[] userA = signupAndGetTokenAndId("userA@noriter.com", "유저A");
        String[] userB = signupAndGetTokenAndId("userB@noriter.com", "유저B");

        createProjectDirectly(Long.parseLong(userA[1]), "guestA", "A의 게임");
        createProjectDirectly(Long.parseLong(userB[1]), "guestB", "B의 게임");

        // 유저A 조회 — A의 게임만
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + userA[0])
                        .header("X-Guest-Id", "guestA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("A의 게임"));

        // 유저B 조회 — B의 게임만
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + userB[0])
                        .header("X-Guest-Id", "guestB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("B의 게임"));
    }

    // ============================================================
    // 2. 로그인 유저 간 삭제/조회 차단
    // ============================================================

    @Test
    @DisplayName("유저B가 유저A의 프로젝트를 삭제하려 하면 403이 반환된다")
    void userB_cannotDelete_userA_project() throws Exception {
        String[] userA = signupAndGetTokenAndId("ownerA@noriter.com", "소유자A");
        String[] userB = signupAndGetTokenAndId("attackB@noriter.com", "공격자B");

        String projectId = createProjectDirectly(Long.parseLong(userA[1]), "guestOwner", "A의 소중한 게임");

        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + userB[0])
                        .header("X-Guest-Id", "guestAttacker"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-O001"));

        assertThat(projectRepository.findById(projectId)).isPresent();
    }

    @Test
    @DisplayName("유저B가 유저A의 프로젝트 상세를 조회하면 403이 반환된다")
    void userB_cannotView_userA_projectDetail() throws Exception {
        String[] userA = signupAndGetTokenAndId("detailA@noriter.com", "상세A");
        String[] userB = signupAndGetTokenAndId("detailB@noriter.com", "상세B");

        String projectId = createProjectDirectly(Long.parseLong(userA[1]), "guestDetailA", "비밀 게임");

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + userB[0])
                        .header("X-Guest-Id", "guestDetailB"))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // 3. 게스트 간 격리
    // ============================================================

    @Test
    @DisplayName("게스트A의 프로젝트는 게스트A만 볼 수 있다")
    void guestA_seesOnlyOwnProjects() throws Exception {
        createProjectDirectly(null, "guest_aaa111", "게스트A 게임");
        createProjectDirectly(null, "guest_bbb222", "게스트B 게임");

        mockMvc.perform(get("/api/projects").header("X-Guest-Id", "guest_aaa111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("게스트A 게임"));

        mockMvc.perform(get("/api/projects").header("X-Guest-Id", "guest_bbb222"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("게스트B 게임"));
    }

    @Test
    @DisplayName("게스트B가 게스트A의 프로젝트를 삭제하려 하면 403이 반환된다")
    void guestB_cannotDelete_guestA_project() throws Exception {
        String projectId = createProjectDirectly(null, "guest_owner_x", "게스트 소유 게임");

        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("X-Guest-Id", "guest_thief_y"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-O001"));

        assertThat(projectRepository.findById(projectId)).isPresent();
    }

    @Test
    @DisplayName("게스트B가 게스트A의 프로젝트 상세를 조회하면 403이 반환된다")
    void guestB_cannotView_guestA_projectDetail() throws Exception {
        String projectId = createProjectDirectly(null, "guest_secret", "비밀 게스트 게임");

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("X-Guest-Id", "guest_spy"))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // 4. 로그인 유저 ↔ 게스트 간 격리
    // ============================================================

    @Test
    @DisplayName("게스트가 로그인 유저의 프로젝트에 접근하면 403이 반환된다")
    void guest_cannotAccess_loggedInUser_project() throws Exception {
        String[] user = signupAndGetTokenAndId("protected@noriter.com", "보호자");
        String projectId = createProjectDirectly(Long.parseLong(user[1]), "guest_protected", "보호된 게임");

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("X-Guest-Id", "guest_random"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("로그인 유저가 게스트의 프로젝트에 접근하면 403이 반환된다")
    void loggedInUser_cannotAccess_guest_project() throws Exception {
        String projectId = createProjectDirectly(null, "guest_lonely", "게스트 혼자 게임");

        String[] user = signupAndGetTokenAndId("intruder@noriter.com", "침입자");
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + user[0])
                        .header("X-Guest-Id", "guest_intruder"))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // 5. 소유자 본인은 정상 작동
    // ============================================================

    @Test
    @DisplayName("로그인 유저 본인은 프로젝트를 정상적으로 삭제할 수 있다")
    void owner_canDelete_ownProject() throws Exception {
        String[] user = signupAndGetTokenAndId("deleter@noriter.com", "삭제자");
        String projectId = createProjectDirectly(Long.parseLong(user[1]), "guest_deleter", "삭제할 게임");

        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + user[0])
                        .header("X-Guest-Id", "guest_deleter"))
                .andExpect(status().isNoContent());

        assertThat(projectRepository.findById(projectId)).isEmpty();
    }

    @Test
    @DisplayName("게스트 본인은 자기 프로젝트를 정상적으로 삭제할 수 있다")
    void guest_canDelete_ownProject() throws Exception {
        String projectId = createProjectDirectly(null, "guest_myown", "내 게임");

        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("X-Guest-Id", "guest_myown"))
                .andExpect(status().isNoContent());

        assertThat(projectRepository.findById(projectId)).isEmpty();
    }
}
