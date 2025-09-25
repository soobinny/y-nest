package com.example.capstonedesign.common.exception;

import lombok.Getter;

/**
 * ErrorCode
 * ---------------------------------------------
 * - API 예외 처리 시 공통으로 사용할 에러 코드 정의 Enum
 * - HTTP 상태 코드 + 기본 메시지를 함께 관리
 * - ApiException과 함께 사용되어 클라이언트에 일관된 에러 응답 제공
 */
@Getter
public enum ErrorCode {

    /** 잘못된 요청 (유효성 검사 실패 등) */
    BAD_REQUEST(400, "Bad Request"),

    /** 인증 실패 (로그인 필요, 토큰 만료 등) */
    UNAUTHORIZED(401, "Unauthorized"),

    /** 권한 없음 (접근 금지) */
    FORBIDDEN(403, "Forbidden"),

    /** 리소스를 찾을 수 없음 */
    NOT_FOUND(404, "Not Found"),

    /** 중복 충돌 (이미 존재하는 데이터 등) */
    CONFLICT(409, "Conflict"),

    /** 서버 내부 에러 (예상치 못한 오류) */
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    /** HTTP 상태 코드 */
    private final int statusCode;

    /** 에러 기본 메시지 */
    private final String message;

    /**
     * ErrorCode 생성자
     *
     * @param statusCode HTTP 상태 코드
     * @param message    에러 기본 메시지
     */
    ErrorCode(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
}
