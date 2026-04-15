package com.noriter.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})  // WebConfig의 CORS 설정 사용
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 인증 불필요 엔드포인트
                .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
                // 게스트도 접근 가능한 엔드포인트 (기존 X-Guest-Id 기반)
                .requestMatchers("/api/projects/**").permitAll()
                .requestMatchers("/api/games/**").permitAll()
                .requestMatchers("/api/logs/**").permitAll()
                .requestMatchers("/api/settings/**").permitAll()
                .requestMatchers("/api/sse/**").permitAll()
                // Actuator 헬스체크
                .requestMatchers("/actuator/**").permitAll()
                // /api/auth/me는 인증 필요
                .requestMatchers("/api/auth/me").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
