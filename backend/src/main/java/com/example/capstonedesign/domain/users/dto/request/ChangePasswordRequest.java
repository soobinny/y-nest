package com.example.capstonedesign.domain.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * ChangePasswordRequest
 * -------------------------------------------------
 * - 비밀번호 변경 요청 시 클라이언트에서 전달하는 DTO
 * - 레코드(record)로 불변 객체 형태를 유지
 * - Bean Validation을 사용해 필드 검증 수행
 */
public record ChangePasswordRequest(

        /*
          현재 비밀번호
          - 빈 값이면 검증 실패
         */
        @NotBlank
        String currentPassword,

        /*
          새 비밀번호
          - 빈 값이면 검증 실패
          - 정규식으로 8자 이상, 대문자/소문자/숫자/특수문자 각 1자 이상 포함 확인
         */
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$",
                message = "비밀번호는 8자 이상, 대소문자/숫자/특수문자를 각각 1자 이상 포함해야 합니다."
        )
        String newPassword
) {}
