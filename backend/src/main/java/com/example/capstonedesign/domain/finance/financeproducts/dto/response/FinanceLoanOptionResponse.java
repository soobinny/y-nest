package com.example.capstonedesign.domain.finance.financeproducts.dto.response;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceLoanOption;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * FinanceLoanOptionResponse
 * - 대출 금리 및 상품 옵션 정보를 담는 응답 DTO
 * - Controller → Client 로 전달되는 데이터 포맷 정의
 */
@Data
@Builder
public class FinanceLoanOptionResponse {

    // 상위 대출 상품 정보
    private String productName;     // 금융상품명
    private String companyName;     // 금융회사명
    private String productType;     // 대출 상품 유형 (MORTGAGE_LOAN, RENT_HOUSE_LOAN, CREDIT_LOAN)

    // 금리 정보
    private BigDecimal lendRateMin;
    private BigDecimal lendRateMax;
    private BigDecimal lendRateAvg;

    // 유형 정보
    private String lendTypeName;   // 금리 유형
    private String rpayTypeName;   // 상환 방식
    private String mrtgTypeName;   // 담보 유형

    /**
     * 엔티티 → DTO 변환 메서드
     * @param option FinanceLoanOption 엔티티
     * @return FinanceLoanOptionResponse DTO
     */
    public static FinanceLoanOptionResponse fromEntity(FinanceLoanOption option) {
        var product = option.getFinanceProduct();   // 하위 금융상품 엔티티
        var baseProduct = product.getProduct();     // 공통 Products 엔티티

        return FinanceLoanOptionResponse.builder()
                .productName(baseProduct != null ? baseProduct.getName() : "이름없음")
                .companyName(baseProduct != null ? baseProduct.getProvider() : "미상") // provider가 금융회사명
                .productType(product.getProductType() != null
                        ? product.getProductType().name()
                        : "UNKNOWN")
                .lendRateMin(option.getLendRateMin())
                .lendRateMax(option.getLendRateMax())
                .lendRateAvg(option.getLendRateAvg())
                .lendTypeName(option.getLendTypeName())
                .rpayTypeName(option.getRpayTypeName())
                .mrtgTypeName(option.getMrtgTypeName())
                .build();
    }
}
