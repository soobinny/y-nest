package com.example.capstonedesign.domain.users.dto.request;

/**
 * UpdateUserRequest
 * -------------------------------------------------
 * - 회원 프로필 수정 요청 시 사용되는 DTO
 * - 선택적으로 나이, 소득 구간, 거주 지역, 무주택 여부를 전달
 * - 모든 필드 nullable로 처리되어, 일부 항목만 업데이트 가능
 */
public record UpdateUserRequest(

        // 나이 (nullable, 변경하지 않으면 null)
        Integer age,

        // 소득 구간 (nullable, 변경하지 않으면 null)
        String income_band,

        // 거주 지역 (nullable, 변경하지 않으면 null)
        String region,

        // 무주택 여부 (nullable, 변경하지 않으면 null)
        Boolean is_homeless
) {}
