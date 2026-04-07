package com.noriter.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noriter.auth.dto.LoginRequest;
import com.noriter.auth.dto.SignupRequest;
import com.noriter.exception.ErrorCode;
import com.noriter.exception.NoriterException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("회원가입 성공 시 201과 토큰을 반환한다")
    void signup_success_returns201() throws Exception {
        AuthResponse response = new AuthResponse("new-token", 1L, "test@noriter.com", "테스터", "USER");
        when(authService.signup("test@noriter.com", "password123", "테스터")).thenReturn(response);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@noriter.com\",\"password\":\"password123\",\"name\":\"테스터\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("new-token"))
                .andExpect(jsonPath("$.email").value("test@noriter.com"))
                .andExpect(jsonPath("$.name").value("테스터"));
    }

    @Test
    @DisplayName("이메일 없이 회원가입 시 400을 반환한다")
    void signup_noEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password123\",\"name\":\"테스터\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호 6자 미만으로 회원가입 시 400을 반환한다")
    void signup_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@noriter.com\",\"password\":\"123\",\"name\":\"테스터\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 이메일 회원가입 시 409를 반환한다")
    void signup_duplicateEmail_returns409() throws Exception {
        when(authService.signup("dup@noriter.com", "password123", "테스터"))
                .thenThrow(new NoriterException(ErrorCode.EMAIL_ALREADY_EXISTS));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@noriter.com\",\"password\":\"password123\",\"name\":\"테스터\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-AU01"));
    }

    @Test
    @DisplayName("로그인 성공 시 200과 토큰을 반환한다")
    void login_success_returns200() throws Exception {
        AuthResponse response = new AuthResponse("login-token", 1L, "user@noriter.com", "유저", "USER");
        when(authService.login("user@noriter.com", "password123")).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@noriter.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-token"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @DisplayName("잘못된 자격증명으로 로그인 시 401을 반환한다")
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login("user@noriter.com", "wrong"))
                .thenThrow(new NoriterException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@noriter.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NT-ERR-AU02"));
    }

    @Test
    @DisplayName("유효한 토큰으로 /me 요청 시 유저 정보를 반환한다")
    void me_withValidToken_returns200() throws Exception {
        when(jwtUtil.isValid("valid-token")).thenReturn(true);
        when(jwtUtil.getUserId("valid-token")).thenReturn(1L);
        when(jwtUtil.getEmail("valid-token")).thenReturn("user@noriter.com");
        when(authService.me(1L)).thenReturn(new AuthResponse(null, 1L, "user@noriter.com", "유저", "USER"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@noriter.com"))
                .andExpect(jsonPath("$.name").value("유저"));
    }

    @Test
    @DisplayName("토큰 없이 /me 요청 시 실패한다")
    void me_withoutToken_returns4xx() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().is4xxClientError());
    }
}
