package com.example.capstonedesign.domain.users.dto.request;

/**
 * 아이디(이메일) 찾기 요청 DTO
 * - 사용자의 이름과 지역 정보를 이용해 계정 조회
 *
 * @param name   사용자 이름 (공백 불가)
 * @param region 사용자 지역 (공백 불가)
 */
public record FindIdRequest(
        @jakarta.validation.constraints.NotBlank String name,
        @jakarta.validation.constraints.NotBlank String region
) {}
