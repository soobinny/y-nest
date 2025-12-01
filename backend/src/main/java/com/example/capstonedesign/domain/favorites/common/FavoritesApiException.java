package com.example.capstonedesign.domain.favorites.common;

import lombok.Getter;

/**
 * FavoritesApiException
 * -------------------------------------------------
 * 즐겨찾기(Favorites) 도메인 전용 사용자 정의 예외 클래스
 * - 비즈니스 로직 중 예외 상황이 발생했을 때, 해당 예외의 유형을
 *   FavoritesErrorCode(Enum)로 명확히 구분하여 전달함.
 * - GlobalExceptionHandler(전역 예외 처리기)에서 이 예외를 감지하여
 *   표준화된 에러 응답(JSON) 형태로 변환해 클라이언트에게 반환.
 */
@Getter
public class FavoritesApiException extends RuntimeException {

    /** 발생한 예외의 구체적 코드 및 메시지를 담는 열거형 */
    private final FavoritesErrorCode errorCode;

    /**
     * 지정된 ErrorCode를 기반으로 예외 객체 생성
     *
     * @param errorCode FavoritesErrorCode (예: FAVORITE_ALREADY_EXISTS)
     *                  → 이 코드에 따라 GlobalExceptionHandler가 HTTP 상태와 메시지를 결정
     */
    public FavoritesApiException(FavoritesErrorCode errorCode) {
        // RuntimeException의 message 필드에 ErrorCode의 메시지를 전달
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}