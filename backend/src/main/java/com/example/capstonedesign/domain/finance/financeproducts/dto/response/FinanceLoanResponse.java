package com.example.capstonedesign.domain.finance.financeproducts.dto.response;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceLoanOption;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * FinanceLoanResponse
 * - 개인신용대출(CREDIT_LOAN) 응답 DTO
 * - 상품 기본정보 + 등급별(1~13등급) 평균 금리 정보 포함
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
public class FinanceLoanResponse {

    private Integer productId;

    /** 상품명 (예: 개인신용대출 A형) */
    private String productName;

    /** 금융회사명 (예: 신한은행, 하나은행 등) */
    private String companyName;

    /** 상품 유형 (FinanceProductType.CREDIT_LOAN) */
    private FinanceProductType productType;

    /** 평균 금리 관련 필드 */
    private BigDecimal lendRateMin;      // 최저 금리
    private BigDecimal lendRateMax;      // 최고 금리
    private BigDecimal lendRateAvg;      // 평균 금리

    /** 금리유형 코드 (예: FIXED, VARIABLE 등) */
    private String crdtLendRateType;

    /** 금리유형 이름 (예: 고정금리, 변동금리 등) */
    private String crdtLendRateTypeNm;

    /** 신용등급별 금리 (1, 4, 5, 6, 10~13등급) */
    private BigDecimal crdtGrad1;
    private BigDecimal crdtGrad4;
    private BigDecimal crdtGrad5;
    private BigDecimal crdtGrad6;
    private BigDecimal crdtGrad10;
    private BigDecimal crdtGrad11;
    private BigDecimal crdtGrad12;
    private BigDecimal crdtGrad13;

    /** 평균 금리 */
    private BigDecimal crdtGradAvg;

    /** 추천 점수 (낮을수록 추천순위 높음) */
    private Double score;
    /** 추천 근거 요약 (예: 청년층 + 저소득 우대) */
    private String reason;

    public static FinanceLoanResponse fromEntity(FinanceLoanOption option) {

    var financeProduct = option.getFinanceProduct();
    var product = financeProduct.getProduct();

    return FinanceLoanResponse.builder()
            .productId(product != null ? product.getId() : null)
            .productName(product != null ? product.getName() : null)
            .companyName(product != null ? product.getProvider() : null)
            .productType(financeProduct.getProductType())

            // 금리 정보
            .lendRateMin(option.getLendRateMin())
            .lendRateMax(option.getLendRateMax())
            .lendRateAvg(option.getLendRateAvg())

            // 금리유형 코드/이름
            .crdtLendRateType(option.getCrdtLendRateType())
            .crdtLendRateTypeNm(option.getCrdtLendRateTypeNm())

            // 등급별 금리
            .crdtGrad1(option.getCrdtGrad1())
            .crdtGrad4(option.getCrdtGrad4())
            .crdtGrad5(option.getCrdtGrad5())
            .crdtGrad6(option.getCrdtGrad6())
            .crdtGrad10(option.getCrdtGrad10())
            .crdtGrad11(option.getCrdtGrad11())
            .crdtGrad12(option.getCrdtGrad12())
            .crdtGrad13(option.getCrdtGrad13())

            // 평균 금리
            .crdtGradAvg(option.getCrdtGradAvg())

            // 추천 점수/사유 (없으면 null)
            .score(null)
            .reason(null)
            .build();
    }
}
