package com.example.capstonedesign.domain.users.dto.response;

import java.util.List;

/**
 * 아이디(이메일) 찾기 응답 DTO
 * - 마스킹된 이메일 목록 반환
 *
 * @param maskedEmails 마스킹된 이메일 문자열 목록
 */
public record FindIdResponse(List<String> maskedEmails) {}