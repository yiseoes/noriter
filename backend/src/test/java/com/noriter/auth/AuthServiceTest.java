package com.noriter.auth;

import com.noriter.domain.User;
import com.noriter.exception.ErrorCode;
import com.noriter.exception.NoriterException;
import com.noriter.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("회원가입 성공 시 토큰과 유저 정보를 반환한다")
    void signup_success() {
        when(userRepository.existsByEmail("new@noriter.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtUtil.generateToken(any(), eq("new@noriter.com"))).thenReturn("mock-token");

        AuthResponse response = authService.signup("new@noriter.com", "password", "테스터");

        assertThat(response.getToken()).isEqualTo("mock-token");
        assertThat(response.getEmail()).isEqualTo("new@noriter.com");
        assertThat(response.getName()).isEqualTo("테스터");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("중복 이메일로 회원가입 시 예외가 발생한다")
    void signup_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("dup@noriter.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup("dup@noriter.com", "password", "테스터"))
                .isInstanceOf(NoriterException.class)
                .satisfies(e -> assertThat(((NoriterException) e).getErrorCode())
                        .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그인 성공 시 토큰과 유저 정보를 반환한다")
    void login_success() {
        User user = User.create("user@noriter.com", "encodedPw", "유저");
        when(userRepository.findByEmail("user@noriter.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPw", "encodedPw")).thenReturn(true);
        when(jwtUtil.generateToken(any(), eq("user@noriter.com"))).thenReturn("login-token");

        AuthResponse response = authService.login("user@noriter.com", "rawPw");

        assertThat(response.getToken()).isEqualTo("login-token");
        assertThat(response.getEmail()).isEqualTo("user@noriter.com");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 예외가 발생한다")
    void login_emailNotFound_throwsException() {
        when(userRepository.findByEmail("none@noriter.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("none@noriter.com", "password"))
                .isInstanceOf(NoriterException.class)
                .satisfies(e -> assertThat(((NoriterException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 예외가 발생한다")
    void login_wrongPassword_throwsException() {
        User user = User.create("user@noriter.com", "encodedPw", "유저");
        when(userRepository.findByEmail("user@noriter.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPw", "encodedPw")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("user@noriter.com", "wrongPw"))
                .isInstanceOf(NoriterException.class)
                .satisfies(e -> assertThat(((NoriterException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    @DisplayName("me() 호출 시 유저 정보를 반환한다 (토큰 없이)")
    void me_success() {
        User user = User.create("user@noriter.com", "encodedPw", "유저");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AuthResponse response = authService.me(1L);

        assertThat(response.getToken()).isNull();
        assertThat(response.getEmail()).isEqualTo("user@noriter.com");
        assertThat(response.getName()).isEqualTo("유저");
    }

    @Test
    @DisplayName("존재하지 않는 userId로 me() 호출 시 예외가 발생한다")
    void me_userNotFound_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(999L))
                .isInstanceOf(NoriterException.class)
                .satisfies(e -> assertThat(((NoriterException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_TOKEN));
    }
}
