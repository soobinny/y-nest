package com.example.capstonedesign.domain.users.dto.response;

/**
 * TokenResponse
 * -------------------------------------------------
 * - 로그인 성공 시 클라이언트에 반환되는 JWT 토큰 정보 DTO
 * - 토큰 문자열, 토큰 타입, 만료 시간 등을 포함
 */
public record TokenResponse(

        // 발급된 JWT 토큰 문자열
        String token,

        // 토큰 타입 (예: "Bearer")
        String tokenType,

        // 토큰 만료까지 남은 시간 (초 단위)
        long expiresInSeconds
) {}
