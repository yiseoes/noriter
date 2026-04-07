package com.noriter.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noriter.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Auth 통합 테스트 — 실제 H2 DB 연동
 * 회원가입 → 로그인 → 토큰 → API 호출 전체 플로우 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // ============================================================
    // 1. 회원가입 플로우
    // ============================================================

    @Test
    @DisplayName("회원가입 → DB에 유저가 저장되고 유효한 JWT 토큰이 발급된다")
    void signup_savesToDbAndReturnsValidToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"hero@noriter.com\",\"password\":\"secure123\",\"name\":\"히어로\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("hero@noriter.com"))
                .andExpect(jsonPath("$.name").value("히어로"))
                .andExpect(jsonPath("$.userId").isNumber())
                .andReturn();

        // DB 저장 확인
        assertThat(userRepository.existsByEmail("hero@noriter.com")).isTrue();

        // 발급된 토큰 유효성 확인
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        assertThat(jwtUtil.isValid(token)).isTrue();
        assertThat(jwtUtil.getEmail(token)).isEqualTo("hero@noriter.com");
    }

    @Test
    @DisplayName("비밀번호는 BCrypt로 해싱되어 평문과 다르게 저장된다")
    void signup_passwordIsHashed() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"hash@noriter.com\",\"password\":\"plain123\",\"name\":\"해시\"}"))
                .andExpect(status().isCreated());

        String storedPassword = userRepository.findByEmail("hash@noriter.com").get().getPassword();
        assertThat(storedPassword).isNotEqualTo("plain123");
        assertThat(storedPassword).startsWith("$2a$");  // BCrypt 접두사
    }

    @Test
    @DisplayName("중복 이메일로 가입하면 409를 반환하고 DB에 중복 저장되지 않는다")
    void signup_duplicateEmail_returns409() throws Exception {
        // 첫 번째 가입 성공
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@noriter.com\",\"password\":\"pass1234\",\"name\":\"원본\"}"))
                .andExpect(status().isCreated());

        // 같은 이메일로 재가입 시도
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@noriter.com\",\"password\":\"pass5678\",\"name\":\"사기꾼\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-AU01"));

        // DB에 1명만 존재
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
    void signup_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"pass1234\",\"name\":\"잘못\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호 6자 미만이면 400을 반환한다")
    void signup_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"short@noriter.com\",\"password\":\"12345\",\"name\":\"짧은\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이름 없이 가입하면 400을 반환한다")
    void signup_noName_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"noname@noriter.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // 2. 로그인 플로우
    // ============================================================

    @Test
    @DisplayName("가입한 계정으로 로그인하면 유효한 토큰이 발급된다")
    void login_afterSignup_returnsValidToken() throws Exception {
        // 가입
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@noriter.com\",\"password\":\"mypass123\",\"name\":\"로그인러\"}"))
                .andExpect(status().isCreated());

        // 로그인
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@noriter.com\",\"password\":\"mypass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("login@noriter.com"))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 401 + 동일한 에러 메시지를 반환한다")
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@noriter.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-AU02"))
                .andExpect(jsonPath("$.error.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("비밀번호가 틀려도 401 + 동일한 에러 메시지를 반환한다 (정보 노출 방지)")
    void login_wrongPassword_returns401_sameMessage() throws Exception {
        // 가입
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"victim@noriter.com\",\"password\":\"correct123\",\"name\":\"피해자\"}"))
                .andExpect(status().isCreated());

        // 틀린 비번으로 로그인
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"victim@noriter.com\",\"password\":\"wrong999\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-AU02"))
                .andExpect(jsonPath("$.error.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    // ============================================================
    // 3. 토큰 인증 플로우 (/me)
    // ============================================================

    @Test
    @DisplayName("가입 → 로그인 → 발급된 토큰으로 /me 호출 → 유저 정보 반환")
    void fullFlow_signup_login_me() throws Exception {
        // 가입
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"full@noriter.com\",\"password\":\"flow1234\",\"name\":\"풀플로우\"}"))
                .andExpect(status().isCreated());

        // 로그인
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"full@noriter.com\",\"password\":\"flow1234\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        // /me 호출
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("full@noriter.com"))
                .andExpect(jsonPath("$.name").value("풀플로우"))
                .andExpect(jsonPath("$.token").doesNotExist());  // me 응답엔 토큰 없음
    }

    @Test
    @DisplayName("토큰 없이 /me 호출하면 인증 실패한다")
    void me_withoutToken_fails() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("잘못된 토큰으로 /me 호출하면 인증 실패한다")
    void me_withInvalidToken_fails() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer fake.invalid.token"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("만료된 토큰으로 /me 호출하면 인증 실패한다")
    void me_withExpiredToken_fails() throws Exception {
        // 만료 시간이 -1초인 JwtUtil로 토큰 생성
        JwtUtil expiredJwtUtil = new JwtUtil("testSecretKeyForJwtTokenGeneration2026TestOnly!@#$", -1000);

        // 가입해서 userId 확보
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"expired@noriter.com\",\"password\":\"pass1234\",\"name\":\"만료\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        Long userId = objectMapper.readTree(signupResult.getResponse().getContentAsString()).get("userId").asLong();
        String expiredToken = expiredJwtUtil.generateToken(userId, "expired@noriter.com");

        // 만료 토큰으로 /me
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().is4xxClientError());
    }
}
