package com.example.capstonedesign.domain.favorites.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {

    @AfterEach
    void tearDown() {
        // 매 테스트 후 컨텍스트 초기화 (다른 테스트에 영향 방지)
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("id - SecurityContext에 principal이 Long이면 그대로 반환한다")
    void id_returns_userId_when_principal_is_long() {
        // given
        Long expectedUserId = 1L;
        var auth = new UsernamePasswordAuthenticationToken(
                expectedUserId,
                "N/A"
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        HttpServletRequest request = new MockHttpServletRequest();

        // when
        Long userId = CurrentUser.id(request);

        // then
        assertThat(userId).isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("id - Authentication이 없으면 IllegalStateException을 던진다")
    void id_throws_when_authentication_is_null() {
        // given
        SecurityContextHolder.clearContext(); // auth = null 상태 보장
        HttpServletRequest request = new MockHttpServletRequest();

        // when & then
        assertThatThrownBy(() -> CurrentUser.id(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated user");
    }

    @Test
    @DisplayName("id - principal이 Long 타입이 아니면 IllegalStateException을 던진다")
    void id_throws_when_principal_is_not_long() {
        // given
        var auth = new UsernamePasswordAuthenticationToken(
                "not-a-long",  // principal: String
                "N/A"
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        HttpServletRequest request = new MockHttpServletRequest();

        // when & then
        assertThatThrownBy(() -> CurrentUser.id(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated user");
    }
}
