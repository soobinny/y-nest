package com.example.capstonedesign.domain.finance.financeproducts.dto.response;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * FinanceProductsResponse DTO
 * - 금융상품(FinanceProducts) + 연관 Products 정보까지 묶어서 내려주는 응답 객체
 * - API 응답 전용 (불변 객체, @Value 사용)
 */
@Value
@Builder
public class FinanceProductsResponse {

    /** finance_products.id (금융 상품 PK) */
    Integer id;

    /** products.id (상품 공통 PK) */
    Integer productId;

    /** 상품명 (products.name) */
    String productName;

    /** 제공자 (products.provider: 은행/저축은행명 등) */
    String provider;

    /** 금융회사 고유번호 (finance_products.fin_co_no) */
    String finCoNo;

    /** 금융상품 유형 (예금/적금) */
    FinanceProductType productType; // DEPOSIT, SAVING

    /** 대표 금리 */
    BigDecimal interestRate;

    /** 최소 예치금 (nullable 가능) */
    Integer minDeposit;

    /** 상세 페이지 URL (products.detail_url) */
    String detailUrl;

    /** 가입 조건 (예: 만 19세 이상, 특정 조건 필요 등) */
    String joinCondition;
}
