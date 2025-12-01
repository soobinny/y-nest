package com.example.capstonedesign.domain.users.dto.request;

import java.time.LocalDate;

import com.example.capstonedesign.domain.users.entity.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * SignupRequest
 * -------------------------------------------------
 * - 회원 가입 요청 시 클라이언트에서 전달되는 DTO
 * - 이메일, 비밀번호, 나이, 소득 구간, 거주 지역, 무주택 여부, 사용자 역할 등 정보를 포함
 * - Bean Validation 으로 필드 유효성 검증 수행
 */
public record SignupRequest(

        /*
          사용자 이메일
          - 반드시 이메일 형식이어야 하며 빈 값은 허용되지 않음
         */
        @Email(message = "유효한 이메일 형식이어야 합니다.")
        @NotBlank
        String email,

        /*
          사용자 비밀번호
          - 8자 이상, 대문자/소문자/숫자/특수문자 각각 최소 1자 포함
          - 정규식을 통해 서버단에서 강제
         */
        @NotBlank
        @Pattern(
                 regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
  message = "비밀번호는 8자 이상, 영문자/숫자/특수문자를 각각 1자 이상 포함해야 합니다."
        )
        String password,

        // 이름 (nullable)
        String name,

        // 나이 (nullable)
        Integer age,

        // 소득 구간 (nullable)
        String income_band,

        //  거주 지역 (nullable)
        String region,

        //  무주택 여부 (nullable, 기본 false)
        Boolean is_homeless,

        //  이메일 공고 수신 동의 여부 (nullable, 기본 true)
        Boolean notificationEnabled,

        //  사용자 역할 (USER 또는 ADMIN, null 허용 안됨)
        @NotNull
        UserRole role,

        // 생년월일 (nullable)
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthdate
) {}
