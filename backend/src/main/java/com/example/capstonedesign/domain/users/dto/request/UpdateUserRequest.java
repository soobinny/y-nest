package com.example.capstonedesign.domain.users.dto.request;

import java.time.LocalDate;

/**
 * UpdateUserRequest
 * -------------------------------------------------
 * - 회원 정보 수정 요청 시 전달되는 DTO
 * - 선택적으로 나이, 소득 구간, 거주 지역, 무주택 여부, 알림 동의를 전달
 * - 모든 필드는 nullable로 처리되어, 일부만 부분 업데이트 가능
 */
public record UpdateUserRequest(

        // 나이 (nullable, 변경하지 않을 땐 null)
        Integer age,

        // 소득 구간 (nullable, 변경하지 않을 땐 null)
        String income_band,

        // 거주 지역 (nullable, 변경하지 않을 땐 null)
        String region,

        // 무주택 여부 (nullable, 변경하지 않을 땐 null)
        Boolean is_homeless,

        // 알림 수신 여부 (nullable, 변경하지 않을 땐 null)
        Boolean notificationEnabled,

        // 생년월일 (nullable)
        LocalDate birthdate
) {}
