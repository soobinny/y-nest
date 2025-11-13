package com.example.capstonedesign.domain.favorites.dto;

import com.example.capstonedesign.domain.products.entity.ProductType;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * FavoritesDto
 * -------------------------------------------------
 * - 즐겨찾기(Favorites) 관련 요청/응답 DTO 모음 클래스
 * - 내부 정적 클래스 형태로 요청(CreateRequest) / 응답(ItemResponse)을 구분
 * <p></p>
 * 역할 요약
 * 1. CreateRequest : 즐겨찾기 추가 요청 본문
 * 2. ItemResponse  : 즐겨찾기 목록 조회 응답
 */
public class FavoritesDto {

    /**
     * CreateRequest
     * -------------------------------------------------
     * - 즐겨찾기 추가 요청용 DTO
     * - 클라이언트에서 전달하는 productId를 검증 및 매핑
     * <p></p>
     * 특징
     * - @NotNull : 필수 입력값 검증
     * - @JsonAlias : snake_case, camelCase 모두 허용
     * - @Schema   : Swagger 문서화용 필드 설명
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateRequest {

        @NotNull(message = "productId는 필수입니다.")
        @JsonAlias({ "product_id", "productId" }) // snake_case / camelCase 모두 허용
        @Schema(
                description = "즐겨찾기 대상 상품/공고 ID",
                example = "123",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Long productId;
    }

    /**
     * ItemResponse
     * -------------------------------------------------
     * - 즐겨찾기 목록 조회 시 응답 데이터 구조
     * - Favorites 엔티티와 Product 정보를 통합하여 반환
     * <p></p>
     * 포함 필드
     * - productId   : 상품/공고 식별자
     * - productName : 상품 또는 공고 제목
     * - provider    : 제공 기관/금융사
     * - detailUrl   : 상세 페이지 URL
     * - createdAt   : 즐겨찾기 등록 시각
     */
    @Builder
    @Getter
    public static class ItemResponse {
        private Long productId;
        private String productName;
        private String provider;
        private String detailUrl;
        private LocalDateTime createdAt;

         //(HOUSING / FINANCE / POLICY)
        private ProductType productType;
    }
}
