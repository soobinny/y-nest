package com.example.capstonedesign.domain.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig
 * -------------------------------------------------
 * - Spring Security 전역 보안 설정 클래스
 * - JWT 기반 인증을 사용하며 CSRF를 비활성화
 * - 경로별 접근 제어(permitAll / authenticated) 정의
 * - JwtAuthenticationFilter를 등록하여 모든 요청에서 JWT 검증
 */
@Configuration
public class SecurityConfig {

    /** JWT 인증 필터 */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * SecurityFilterChain 설정
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain
     * @throws Exception Spring Security 설정 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // JWT 기반이므로 CSRF 토큰 사용 불필요, 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 요청 경로별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 경로
                        .requestMatchers(
                                "/users/signup",            // 회원가입
                                "/users/login",             // 로그인
                                "/v3/api-docs/**",          // Swagger API 문서
                                "/swagger-ui/**",           // Swagger UI
                                "/swagger-ui.html"          // Swagger UI html
                        ).permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                );

        // JWT 인증 필터 등록
        // UsernamePasswordAuthenticationFilter 실행 전에 JWT 토큰 검증
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
