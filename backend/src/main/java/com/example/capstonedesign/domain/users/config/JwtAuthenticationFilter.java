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
 * - 검증 실패 시 401 Unauthorized 응답 반환
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 토큰 생성 및 검증을 담당하는 유틸리티 클래스 */
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * doFilterInternal
     * -------------------------------------------------
     * - 모든 요청마다 실행되는 필터
     * - Authorization 헤더에서 JWT를 추출 후 검증 수행
     * - 유효한 토큰인 경우 SecurityContext에 인증 정보 등록
     * - 잘못된 토큰인 경우 401 Unauthorized 응답
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Authorization 헤더 추출
        String header = request.getHeader("Authorization");

        // 2. Bearer 토큰 존재 여부 확인
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                // 3. JWT 검증 및 Claims 파싱
                Jws<Claims> claims = jwtTokenProvider.parse(token);

                // 4. 토큰에서 사용자 정보 추출
                Long userId = claims.getBody().get("userId", Number.class).longValue();
                String role  = claims.getBody().get("role", String.class); // 예: "USER", "ADMIN"

                // 5. 권한 정보 생성 (Spring Security의 ROLE 규칙에 맞게 접두어 추가)
                List<GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role));

                // 6. 인증 객체 생성 및 SecurityContext 등록
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException e) {
                // 7. 유효하지 않거나 만료된 JWT인 경우 401 반환
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않거나 만료된 JWT입니다.");
                return;
            }
        }

        // 8. 다음 필터 체인으로 요청 전달
        filterChain.doFilter(request, response);
    }
}
