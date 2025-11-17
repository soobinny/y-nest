package com.example.capstonedesign.domain.finance.financeproducts.dto.response;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceLoanOption;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * RentLoanResponse
 * - 전세자금대출(RENT_HOUSE_LOAN) 응답 DTO
 * - 상품 기본정보 + 금리 및 상환 방식 정보 포함
 */
@Data
@Builder
public class RentLoanResponse {

    private Integer productId;

    /** 상품명 (예: 전세자금대출 A형) */
    private String productName;

    /** 금융회사명 (예: 신한은행, 우리은행 등) */
    private String companyName;

    /** 상품 유형 (FinanceProductType.RENT_HOUSE_LOAN) */
    private FinanceProductType productType;

    /** 최소 금리 */
    private BigDecimal lendRateMin;

    /** 최대 금리 */
    private BigDecimal lendRateMax;

    /** 평균 금리 */
    private BigDecimal lendRateAvg;

    /** 금리 유형 (예: 고정금리, 변동금리 등) */
    private String lendTypeName;

    /** 상환 방식 (예: 원리금균등, 만기일시 등) */
    private String rpayTypeName;

public static RentLoanResponse fromEntity(FinanceLoanOption option) {

    var financeProduct = option.getFinanceProduct();
    var product = financeProduct.getProduct();

    return RentLoanResponse.builder()
            .productId(product != null ? product.getId() : null)
            .productName(product != null ? product.getName() : null)
            .companyName(product != null ? product.getProvider() : null)
            .productType(financeProduct.getProductType())
            .lendRateMin(option.getLendRateMin())
            .lendRateMax(option.getLendRateMax())
            .lendRateAvg(option.getLendRateAvg())
            .lendTypeName(option.getLendTypeName())
            .rpayTypeName(option.getRpayTypeName())
            .build();
}
}
