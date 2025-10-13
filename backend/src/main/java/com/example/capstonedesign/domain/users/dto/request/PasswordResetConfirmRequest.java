package com.example.capstonedesign.domain.users.dto.request;

/**
 * 비밀번호 재설정 확정 요청 DTO
 * - 사용자가 받은 토큰을 검증하고 새 비밀번호로 변경할 때 사용
 *
 * @param token       비밀번호 재설정 토큰
 * @param newPassword 새 비밀번호
 */
public record PasswordResetConfirmRequest(
        @jakarta.validation.constraints.NotBlank String token,
        @jakarta.validation.constraints.NotBlank String newPassword
) {}
