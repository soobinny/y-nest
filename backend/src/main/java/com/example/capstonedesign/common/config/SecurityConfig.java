package com.example.capstonedesign.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (API 서버에서는 주로 끔)
                .csrf(csrf -> csrf.disable())

                // CORS 활성화 (CorsConfig랑 연동)
                .cors(Customizer.withDefaults())

                // 요청별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/users/signup",  // 회원가입 허용
                                "/users/login",   // 로그인 허용
                                "/swagger-ui/**", // swagger 접근 허용
                                "/v3/api-docs/**" // API docs 허용
                        ).permitAll()
                        .anyRequest().authenticated() // 그 외는 인증 필요
                );

        return http.build();
    }
}
