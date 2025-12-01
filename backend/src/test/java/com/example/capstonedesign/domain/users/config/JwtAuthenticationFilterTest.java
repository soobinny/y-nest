package com.example.capstonedesign.domain.users.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 단위 테스트
 * -------------------------------------------------
 * - 유효한 JWT가 있을 때 SecurityContext에 인증 정보가 세팅되고
 *   다음 필터 체인으로 정상 위임되는지 검증
 * - 잘못된 JWT일 때 401 응답이 반환되고 필터 체인이 중단되는지 검증
 * - Authorization 헤더가 없을 때 필터가 통과(pass-through) 동작을 하는지 검증
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    FilterChain filterChain;

    @InjectMocks
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        // 테스트 간 SecurityContext 오염 방지
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이 있으면 SecurityContext에 인증 정보가 설정되고 체인이 계속 진행된다")
    void doFilterInternal_validToken_setsAuthenticationAndContinuesChain()
            throws ServletException, IOException {

        // given
        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer " + token);

        // JwtTokenProvider.parse()가 반환할 Jws<Claims>와 Claims mock
        @SuppressWarnings("unchecked")
        Jws<Claims> jws = (Jws<Claims>) mock(Jws.class);
        Claims claims = mock(Claims.class);

        when(jwtTokenProvider.parse(token)).thenReturn(jws);
        when(jws.getBody()).thenReturn(claims);
        when(claims.get("userId", Number.class)).thenReturn(1L);
        when(claims.get("role", String.class)).thenReturn("USER");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        // SecurityContext에 Authentication이 설정되어야 함
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(1L);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        // 다음 필터로 위임되었는지 확인
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("잘못된 JWT가 들어오면 401 Unauthorized를 반환하고 체인을 중단한다")
    void doFilterInternal_invalidToken_returns401AndStopsChain()
            throws ServletException, IOException {

        // given
        String token = "invalid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenProvider.parse(token))
                .thenThrow(new JwtException("유효하지 않은 토큰"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        // 401 상태 코드가 설정되어야 함
        assertThat(response.getStatus()).isEqualTo(401);

        // SecurityContext에는 인증 정보가 없어야 함
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        // 잘못된 토큰이므로 다음 필터는 호출되면 안 된다
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 JWT 검증 없이 체인을 그대로 통과한다")
    void doFilterInternal_noAuthorizationHeader_passThrough()
            throws ServletException, IOException {

        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // Authorization 헤더 미설정

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        // SecurityContext에는 여전히 인증 정보가 없어야 함
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        // 필터 체인은 정상적으로 호출되어야 한다
        verify(filterChain, times(1)).doFilter(request, response);

        // JwtTokenProvider.parse()는 호출되지 않아야 한다
        verify(jwtTokenProvider, never()).parse(anyString());
    }
}
