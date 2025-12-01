package com.example.capstonedesign.domain.favorites.common;

import lombok.Builder;
import lombok.Getter;

/**
 * FavoritesApiErrorResponse
 * -------------------------------------------------
 * 즐겨찾기(Favorites) 도메인에서 발생한 예외를 클라이언트에게 전달하기 위한 표준화된 에러 응답 포맷 클래스
 * - GlobalExceptionHandler 에서 FavoritesApiException을 캐치하면,
 *   해당 예외의 FavoritesErrorCode를 기반으로 이 객체를 생성하여 반환

 */
@Getter
@Builder
public class FavoritesApiErrorResponse {

    /** 예외 코드 이름 (Enum name) - ex. FAVORITE_ALREADY_EXISTS */
    private final String code;

    /** 사용자에게 보여줄 에러 메시지 - ex. 이미 즐겨찾기되었습니다. */
    private final String message;
}