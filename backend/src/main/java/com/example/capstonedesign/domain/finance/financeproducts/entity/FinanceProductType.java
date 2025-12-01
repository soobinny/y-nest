package com.example.capstonedesign.domain.finance.financeproducts.entity;

import lombok.Getter;

/**
 * 금융 상품의 종류를 정의하는 Enum 클래스
 * <p></p>
 * 각 항목은 금융감독원 Open API 호출 시 사용되는 endpoint 이름과
 * 사용자 화면에 표시할 한글명을 함께 가짐
 */
@Getter
public enum FinanceProductType {

    // 정기예금 상품 (예: 은행 예금)
    DEPOSIT("depositProductsSearch", "정기예금"),

    // 적금 상품 (예: 일정 금액을 매달 납입)
    SAVING("savingProductsSearch", "적금"),

    // 주택담보대출 상품 (예: 부동산 담보대출)
    MORTGAGE_LOAN("mortgageLoanProductsSearch", "주택담보대출"),

    // 전세자금대출 상품 (예: 전세 보증금 마련용 대출)
    RENT_HOUSE_LOAN("rentHouseLoanProductsSearch", "전세자금대출"),

    // 개인신용대출 상품 (예: 무담보 신용대출)
    CREDIT_LOAN("creditLoanProductsSearch", "개인신용대출");

    /** 금융감독원 OpenAPI 호출용 엔드포인트 이름 */
    private final String endpoint;

    /** 사용자 화면 표시용 한글 이름 */
    private final String displayName;

    FinanceProductType(String endpoint, String displayName) {
        this.endpoint = endpoint;
        this.displayName = displayName;
    }
}