package com.example.capstonedesign.domain.finance.financeproducts.dto.response;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * FinanceProductsResponse DTO
 * - 금융상품(FinanceProducts) + 연관 Products 정보까지 묶어서 내려주는 응답 객체
 * - API 응답 전용 (불변 객체, @Value 사용)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
public class FinanceProductsResponse {

    /** finance_products.id (금융 상품 PK) */
    private Integer id;

    /** products.id (상품 공통 PK) */
    private Integer productId;

    /** 상품명 (products.name) */
    private String productName;

    /** 제공자 (products.provider: 은행/저축은행명 등) */
    private String provider;

    /** 금융회사 고유번호 (finance_products.fin_co_no) */
    private String finCoNo;

    /** 금융상품 유형 (예금/적금) */
    private FinanceProductType productType; // DEPOSIT, SAVING

    /** 대표 금리 */
    private BigDecimal interestRate;

    /** 최소 예치금 (nullable 가능) */
    private Integer minDeposit;

    /** 상세 페이지 URL (products.detail_url) */
    private String detailUrl;

    /** 가입 조건 (예: 만 19세 이상, 특정 조건 필요 등) */
    private String joinCondition;

    /** 추천 점수 (낮을수록 추천순위 높음) */
    private Double score;
    /** 추천 근거 요약 (예: "청년층 우대, 고금리 상품") */
    private String reason;
}
