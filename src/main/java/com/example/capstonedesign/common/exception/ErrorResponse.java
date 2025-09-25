package com.example.capstonedesign.common.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * ErrorResponse
 * ---------------------------------------------
 * - API 에러 응답을 표준화하기 위한 DTO 클래스
 * - 클라이언트는 항상 동일한 형태로 에러를 받을 수 있음
 *   {
 *      "statusCode": 400,
 *      "message": "Bad Request"
 *   }
 */
@Setter
@Getter
public class ErrorResponse {

    /** HTTP 상태 코드 (예: 400, 401, 403, 500 등) */
    private int statusCode;

    /** 에러 메시지 (기본 또는 커스텀) */
    private String message;

    /**
     * ErrorResponse 생성자
     *
     * @param statusCode HTTP 상태 코드
     * @param message    에러 메시지
     */
    public ErrorResponse(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
}
