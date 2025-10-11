package com.example.capstonedesign.domain.favorites.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * FavoritesErrorCode
 * -------------------------------------------------
 * 즐겨찾기(Favorites) 도메인에서 발생할 수 있는
 * 예외 상황들을 표준화된 형태로 관리하기 위한 열거형(Enum) 클래스
 */
@Getter
public enum FavoritesErrorCode {

    /**
     * 사용자가 이미 해당 상품을 즐겨찾기에 추가한 경우
     * - 중복 요청 방지용 (idempotent 처리)
     * - HTTP 409 CONFLICT
     */
    FAVORITE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 즐겨찾기되었습니다."),

    /**
     * 즐겨찾기 항목을 찾을 수 없는 경우
     * - 삭제된 항목에 접근하거나 존재하지 않는 ID 요청 시 발생
     * - HTTP 404 NOT FOUND
     */
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "즐겨찾기 항목이 없습니다."),

    /**
     * 요청한 userId에 해당하는 사용자가 존재하지 않는 경우
     * - 즐겨찾기 추가 시 유효하지 않은 사용자 ID 전달 시 발생
     * - HTTP 404 NOT FOUND
     */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    /**
     * 요청한 productId에 해당하는 상품이 존재하지 않는 경우
     * - 잘못된 상품 ID를 즐겨찾기하려 할 때 발생
     * - HTTP 404 NOT FOUND
     */
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),

    /**
     * 요청 파라미터나 본문이 유효하지 않거나 누락된 경우
     * - 예: productId = null
     * - HTTP 400 BAD REQUEST
     */
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.");

    /** HTTP 상태 코드 */
    private final HttpStatus status;

    /** 사용자에게 반환할 에러 메시지 */
    private final String message;

    FavoritesErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
