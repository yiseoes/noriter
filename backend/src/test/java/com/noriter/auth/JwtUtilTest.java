package com.noriter.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("testSecretKeyForJwtTokenGeneration2026TestOnly!@#$", 3600000);
    }

    @Test
    @DisplayName("토큰을 생성하면 null이 아닌 문자열을 반환한다")
    void generateToken_returnsNonNullString() {
        String token = jwtUtil.generateToken(1L, "test@noriter.com");

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("생성된 토큰에서 userId를 추출할 수 있다")
    void getUserId_returnsCorrectUserId() {
        String token = jwtUtil.generateToken(42L, "test@noriter.com");

        assertThat(jwtUtil.getUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("생성된 토큰에서 email을 추출할 수 있다")
    void getEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken(1L, "hello@noriter.com");

        assertThat(jwtUtil.getEmail(token)).isEqualTo("hello@noriter.com");
    }

    @Test
    @DisplayName("유효한 토큰은 isValid가 true를 반환한다")
    void isValid_withValidToken_returnsTrue() {
        String token = jwtUtil.generateToken(1L, "test@noriter.com");

        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("잘못된 토큰은 isValid가 false를 반환한다")
    void isValid_withInvalidToken_returnsFalse() {
        assertThat(jwtUtil.isValid("invalid.token.here")).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 토큰은 isValid가 false를 반환한다")
    void isValid_withEmptyToken_returnsFalse() {
        assertThat(jwtUtil.isValid("")).isFalse();
    }

    @Test
    @DisplayName("다른 시크릿으로 생성된 토큰은 검증 실패한다")
    void isValid_withDifferentSecret_returnsFalse() {
        JwtUtil otherJwtUtil = new JwtUtil("anotherSecretKeyThatIsDifferentFromOriginal!@#$", 3600000);
        String token = otherJwtUtil.generateToken(1L, "test@noriter.com");

        assertThat(jwtUtil.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 isValid가 false를 반환한다")
    void isValid_withExpiredToken_returnsFalse() {
        JwtUtil expiredJwtUtil = new JwtUtil("testSecretKeyForJwtTokenGeneration2026TestOnly!@#$", -1000);
        String token = expiredJwtUtil.generateToken(1L, "test@noriter.com");

        assertThat(jwtUtil.isValid(token)).isFalse();
    }
}
