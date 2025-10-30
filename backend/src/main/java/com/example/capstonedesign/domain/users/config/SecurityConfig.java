package com.example.capstonedesign.domain.users.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                // ✅ CORS 설정 추가
                .cors(Customizer.withDefaults())

                // 요청 경로별 권한 설정
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/users/signup",
                                "/users/login",
                                "/users/find-id",
                                "/users/password-reset/**",
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
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/error" // 에러 페이지 접근 허용(로그에 뜨던 /error 403 방지용)
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