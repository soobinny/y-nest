package com.example.capstonedesign.domain.users.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // ============================
    // CORS 설정 (Spring Security가 직접 적용)
    // ============================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 1) 일단 패턴으로 전부 허용 (디버그용)
        config.setAllowedOriginPatterns(Arrays.asList("*"));

        // 2) 메서드 전부 허용
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 3) 헤더 전부 허용
        config.setAllowedHeaders(Arrays.asList("*"));

        // 4) 필요하면 true 로 (쿠키 안 쓰면 false여도 큰 상관 X)
        config.setAllowCredentials(true);

        // 5) /api/** 로 제한 (원하면 /** 도 가능)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    // ============================
    // Spring Security 설정
    // ============================
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/users/signup",
                                "/api/users/login",

                                // 아이디 찾기 관련 (본인 확인 인증 포함)
                                "/api/users/find-id",
                                "/api/users/find-id/**",

                                // 비밀번호 재설정 관련
                                "/api/users/password-reset/**",

                                // 이메일 인증 관련 (이메일 인증번호 발송/확인 등)
                                "/email/**",
                                "/test/email", // Swagger SMTP 테스트용

                                // 공개 API
                                "/api/finance/products/**",

                                // Swagger & 문서 접근 허용
                                "/api/chat",
                                "/api/notices/recent",
                                "/api/housings",
                                "/api/housings/search",
                                "/api/housings/recent",
                                "/api/housings/closing-soon",
                                "/api/sh/housings",
                                "/api/sh/housings/search",
                                "/api/sh/housings/recommend",
                                "/api/sh/housings/recent",
                                "/api/finance/products",
                                "/api/finance/loans/options/type/*",
                                "/api/youth-policies",
                                "/api/youth-policies/*",
                                "/api/youth-policies/recent",
                                "/api/youth-policies/closing-soon",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",

                                // 에러 페이지 접근 허용
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        // 인증 자체가 없을 때 → 401
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다."))
                        // 인증은 되었는데 권한이 없을 때 → 403
                        .accessDeniedHandler((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_FORBIDDEN, "접근 권한이 없습니다."))
                )
                // 커스텀 JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}