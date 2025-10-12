package com.example.capstonedesign.domain.favorites.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * CurrentUser
 * -------------------------------------------------
 * - 현재 인증된 사용자 정보를 가져오는 유틸리티 클래스
 * - SecurityContextHolder 에서 사용자 ID(principal)를 추출
 * - 인증되지 않은 경우 IllegalStateException 발생
 */
public final class CurrentUser {

    /** 유틸리티 클래스이므로 인스턴스화 방지 */
    private CurrentUser() {}

    /**
     * id
     * -------------------------------------------------
     * - SecurityContextHolder 에서 Authentication 객체를 조회하고
     *   principal로 저장된 userId(Long)를 반환
     *
     * @param request 현재 요청 객체 (HttpServletRequest)
     * @return 인증된 사용자 ID (Long)
     * @throws IllegalStateException 인증 정보가 존재하지 않거나 principal 타입이 잘못된 경우
     */
    public static Long id(HttpServletRequest request) {
        // 1. SecurityContext 에서 인증 객체(Authentication) 조회
        var auth = SecurityContextHolder.getContext().getAuthentication();

        // 2️. principal이 Long 타입(userId)인 경우 그대로 반환
        if (auth != null && auth.getPrincipal() instanceof Long l) return l;

        // 3️. 인증되지 않은 요청이거나 principal이 예상 타입이 아닐 경우 예외 발생
        throw new IllegalStateException("No authenticated user");
    }
}