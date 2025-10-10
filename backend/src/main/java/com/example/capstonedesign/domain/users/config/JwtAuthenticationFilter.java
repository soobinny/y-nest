package com.example.capstonedesign.domain.users.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JwtAuthenticationFilter
 * -------------------------------------------------
 * - 모든 HTTP 요청에서 Authorization 헤더를 확인
 * - Bearer 토큰이 존재하면 JWT 검증 수행
 * - 검증 성공 시 Spring Security의 SecurityContext에 인증 객체(Authentication) 등록
 * - 검증 실패 시 401 Unauthorized 응답
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 토큰 발급/검증 유틸 */
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 요청 헤더에서 Authorization 확인
        String header = request.getHeader("Authorization");

        // Bearer 토큰 존재 시
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // "Bearer " 제거 후 순수 토큰

            try {
                // JWT 파싱 및 검증
                Jws<Claims> claims = jwtTokenProvider.parse(token);
                String email = claims.getBody().getSubject(); // 토큰 subject(email) 추출

                // 인증 객체 생성
                // 권한(role)은 필요 시 claims 에서 추출해 추가 가능
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(email, null, List.of());

                // SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException e) {
                // 토큰 유효하지 않거나 만료 시 401 Unauthorized 반환
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않거나 만료된 JWT입니다.");
                return;
            }
        }

        // 필터 체인 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}
