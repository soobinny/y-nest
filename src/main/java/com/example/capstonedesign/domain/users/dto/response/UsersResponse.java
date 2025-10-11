package com.example.capstonedesign.domain.users.dto.response;

import com.example.capstonedesign.domain.users.entity.UserRole;

/**
 * UsersResponse
 * -------------------------------------------------
 * - 사용자 정보를 클라이언트로 반환할 때 사용되는 DTO
 * - 회원 가입, 로그인 후 프로필 조회, 수정 등의 응답에 사용
 * - 비밀번호 등 민감 정보는 포함하지 않음
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

        // 사용자 역할 (USER 또는 ADMIN)
        UserRole role
) {}
