package com.example.capstonedesign.domain.users.dto.request;

/**
 * 비밀번호 재설정 요청 DTO
 * - 사용자의 이메일을 입력받아 비밀번호 재설정 메일을 전송 요청
 *
 * @param email 비밀번호 재설정을 요청할 사용자 이메일
 */
public record PasswordResetRequest(
        @jakarta.validation.constraints.Email String email
) {}
