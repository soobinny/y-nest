package com.example.capstonedesign.domain.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DeleteRequest
 * -------------------------------------------------
 * - 회원 탈퇴 요청 시 사용되는 DTO
 * - 이메일과 비밀번호를 받아 본인 확인 후 탈퇴 처리
 * - Bean Validation 적용으로 필드 유효성 검증
 */
public record DeleteRequest(

        /*
          사용자 이메일
          - 반드시 이메일 형식이어야 하며 빈 값은 허용되지 않음
         */
        @Email
        @NotBlank
        String email,

        /*
          사용자 비밀번호
          - 탈퇴 확인용
          - 빈 값이면 검증 실패
         */
        @NotBlank
        String password
) {}
