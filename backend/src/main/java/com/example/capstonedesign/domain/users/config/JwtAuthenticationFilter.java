package com.example.capstonedesign.domain.users.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Jws<Claims> claims = jwtTokenProvider.parse(token);

                String email = claims.getBody().getSubject();                 // 예: user@example.com
                String role  = claims.getBody().get("role", String.class); // 예: "USER", "ADMIN"

                // 스프링의 hasRole("USER") 규칙과 맞추려면 ROLE_ 접두어 필수
                List<GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않거나 만료된 JWT입니다.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
