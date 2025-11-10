package com.example.capstonedesign.domain.users.dto.response;

import java.time.LocalDate;

import com.example.capstonedesign.domain.users.entity.UserRole;

/**
 * UsersResponse
 * -------------------------------------------------
 * - 사용자 정보를 클라이언트로 반환할 때 사용하는 DTO
 * - 회원 가입, 로그인 후 본인 조회, 정보 수정 응답에 활용
 * - 비밀번호 등 민감 정보는 포함하지 않는다
 */
public record UsersResponse(

        // 사용자 고유 ID
        Integer id,

        // 사용자 이메일
        String email,

        // 사용자 이름
        String name,

        // 나이 (nullable)
        Integer age,

        // 소득 구간 (nullable)
        String income_band,

        // 거주 지역 (nullable)
        String region,

        // 무주택 여부
        Boolean is_homeless,

        // 알림 수신 여부
        Boolean notificationEnabled,

        // 생년월일
        LocalDate birthdate,

        // 사용자 역할 (USER 또는 ADMIN)
        UserRole role
) {}
