package com.example.capstonedesign.domain.finance.financeproducts.dto.response;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceLoanOption;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * MortgageLoanResponse
 * - 주택담보대출(MORTGAGE_LOAN) 응답 DTO
 * - 상품 기본정보 + 금리/상환/담보 옵션 정보 포함
 */
@Data
@Builder
public class MortgageLoanResponse {

     private Integer productId;

    /** 상품명 (예: 주택담보대출 A형) */
    private String productName;

    /** 금융회사명 (예: 우리은행, 국민은행 등) */
    private String companyName;

    /** 상품 유형 (FinanceProductType.MORTGAGE_LOAN) */
    private FinanceProductType productType;

    /** 최소 금리 */
    private BigDecimal lendRateMin;

    /** 최대 금리 */
    private BigDecimal lendRateMax;

    /** 평균 금리 */
    private BigDecimal lendRateAvg;

    /** 금리 유형 (예: 고정, 변동 등) */
    private String lendTypeName;

    /** 상환 방식 (예: 원리금균등, 만기일시 등) */
    private String rpayTypeName;

    /** 담보 유형 (예: 아파트, 주택 등) */
    private String mrtgTypeName;


public static MortgageLoanResponse fromEntity(FinanceLoanOption option) {

    var financeProduct = option.getFinanceProduct();
    var product = financeProduct.getProduct();

    return MortgageLoanResponse.builder()
            .productId(product != null ? product.getId() : null)
            .productName(product != null ? product.getName() : null)
            .companyName(product != null ? product.getProvider() : null)
            .productType(financeProduct.getProductType())
            .lendRateMin(option.getLendRateMin())
            .lendRateMax(option.getLendRateMax())
            .lendRateAvg(option.getLendRateAvg())
            .lendTypeName(option.getLendTypeName())
            .rpayTypeName(option.getRpayTypeName())
            .mrtgTypeName(option.getMrtgTypeName())
            .build();
}
}