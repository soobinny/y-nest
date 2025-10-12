package com.example.capstonedesign.common.exception;

import lombok.Getter;

/**
 * ApiException
 * ---------------------------------------------
 * - 프로젝트 전역에서 발생하는 공통 예외 처리 클래스
 * - RuntimeException을 상속받아 언체크 예외로 동작
 * - ErrorCode와 함께 예외를 던져 일관된 에러 응답을 제공
 */
@Getter
public class ApiException extends RuntimeException {

    /**
     * 에러 코드 (상태 코드, 메시지 등 포함)
     */
    private final ErrorCode errorCode;

    /**
     * ErrorCode 기반 생성자
     * - ErrorCode 내 정의된 기본 메시지를 사용
     *
     * @param errorCode 예외와 매핑되는 에러 코드
     */
    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode + 커스텀 메시지 생성자
     * - 같은 ErrorCode 라도 상황에 따라 다른 메시지를 내려줄 수 있음
     *
     * @param errorCode 예외와 매핑되는 에러 코드
     * @param message   사용자 정의 메시지
     */
    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
