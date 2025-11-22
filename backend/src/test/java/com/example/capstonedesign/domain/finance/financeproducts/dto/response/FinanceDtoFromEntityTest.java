package com.example.capstonedesign.domain.finance.financeproducts.dto.response;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceLoanOption;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FinanceDtoFromEntityTest
 * -------------------------------------------------
 * - 금융 도메인의 fromEntity 매핑 로직 단위 테스트
 * - 엔티티 ↔ DTO 변환이 정확한지 검증
 */
class FinanceDtoFromEntityTest {

    // -----------------------------
    // 공통 더미 생성 헬퍼
    // -----------------------------
    private Products createProducts() {
        return Products.builder()
                .id(10)
                .type(ProductType.FINANCE)
                .name("청년 전세자금대출 A형")
                .provider("신한은행")
                .detailUrl("https://example.com/loan/10")
                .build();
    }

    private FinanceProducts createFinanceProducts(Products product, FinanceProductType type) {
        return FinanceProducts.builder()
                .id(100)
                .product(product)            // 연관 Products
                .finCoNo("0010001")
                .productType(type)
                .interestRate(BigDecimal.valueOf(3.25))
                .minDeposit(1_000_000)
                .joinCondition("만 19세 이상, 청년 전용")
                .build();
    }

    private FinanceLoanOption createFinanceLoanOption(FinanceProducts fp) {
        return FinanceLoanOption.builder()
                .id(200)
                .financeProduct(fp)
                .lendRateMin(BigDecimal.valueOf(3.0))
                .lendRateMax(BigDecimal.valueOf(4.0))
                .lendRateAvg(BigDecimal.valueOf(3.5))
                .lendTypeName("고정금리")
                .rpayTypeName("원리금균등상환")
                .mrtgTypeName("전세보증금담보")
                // 신용대출용 필드 (FinanceLoanResponse용)
                .crdtLendRateType("FIXED")
                .crdtLendRateTypeNm("고정금리")
                .crdtGrad1(BigDecimal.valueOf(3.1))
                .crdtGrad4(BigDecimal.valueOf(3.3))
                .crdtGrad5(BigDecimal.valueOf(3.4))
                .crdtGrad6(BigDecimal.valueOf(3.5))
                .crdtGrad10(BigDecimal.valueOf(3.8))
                .crdtGrad11(BigDecimal.valueOf(3.9))
                .crdtGrad12(BigDecimal.valueOf(4.0))
                .crdtGrad13(BigDecimal.valueOf(4.1))
                .crdtGradAvg(BigDecimal.valueOf(3.7))
                .build();
    }

    // -------------------------------------------------
    // [1] FinanceLoanOptionResponse.fromEntity 테스트
    // -------------------------------------------------
    @Test
    @DisplayName("FinanceLoanOptionResponse.fromEntity - 연관 Product 포함 정상 매핑")
    void financeLoanOptionResponse_fromEntity_withProduct() {
        // given
        Products product = createProducts();
        FinanceProducts fp = createFinanceProducts(product, FinanceProductType.RENT_HOUSE_LOAN);
        FinanceLoanOption option = createFinanceLoanOption(fp);

        // when
        FinanceLoanOptionResponse dto = FinanceLoanOptionResponse.fromEntity(option);

        // then
        assertThat(dto.getProductId()).isEqualTo(product.getId());
        assertThat(dto.getProductName()).isEqualTo(product.getName());
        assertThat(dto.getCompanyName()).isEqualTo(product.getProvider());
        assertThat(dto.getProductType()).isEqualTo(FinanceProductType.RENT_HOUSE_LOAN.name());

        assertThat(dto.getLendRateMin()).isEqualTo(BigDecimal.valueOf(3.0));
        assertThat(dto.getLendRateMax()).isEqualTo(BigDecimal.valueOf(4.0));
        assertThat(dto.getLendRateAvg()).isEqualTo(BigDecimal.valueOf(3.5));
        assertThat(dto.getLendTypeName()).isEqualTo("고정금리");
        assertThat(dto.getRpayTypeName()).isEqualTo("원리금균등상환");
        assertThat(dto.getMrtgTypeName()).isEqualTo("전세보증금담보");
    }

    @Test
    @DisplayName("FinanceLoanOptionResponse.fromEntity - Product가 null이어도 NPE 없이 null 매핑")
    void financeLoanOptionResponse_fromEntity_withoutProduct() {
        // given
        FinanceProducts fp = createFinanceProducts(null, FinanceProductType.RENT_HOUSE_LOAN);
        FinanceLoanOption option = createFinanceLoanOption(fp);

        // when
        FinanceLoanOptionResponse dto = FinanceLoanOptionResponse.fromEntity(option);

        // then
        assertThat(dto.getProductId()).isNull();
        assertThat(dto.getProductName()).isNull();
        assertThat(dto.getCompanyName()).isNull();
        // productType은 FinanceProducts의 필드이므로 그대로 유지
        assertThat(dto.getProductType()).isEqualTo(FinanceProductType.RENT_HOUSE_LOAN.name());
    }

    // -------------------------------------------------
    // [2] FinanceLoanResponse.fromEntity 테스트
    // -------------------------------------------------
    @Test
    @DisplayName("FinanceLoanResponse.fromEntity - 개인신용대출 옵션 매핑")
    void financeLoanResponse_fromEntity() {
        // given
        Products product = createProducts();
        FinanceProducts fp = createFinanceProducts(product, FinanceProductType.CREDIT_LOAN);
        FinanceLoanOption option = createFinanceLoanOption(fp);

        // when
        FinanceLoanResponse dto = FinanceLoanResponse.fromEntity(option);

        // then
        assertThat(dto.getProductId()).isEqualTo(product.getId());
        assertThat(dto.getProductName()).isEqualTo(product.getName());
        assertThat(dto.getCompanyName()).isEqualTo(product.getProvider());
        assertThat(dto.getProductType()).isEqualTo(FinanceProductType.CREDIT_LOAN);

        // 금리 정보
        assertThat(dto.getLendRateMin()).isEqualTo(option.getLendRateMin());
        assertThat(dto.getLendRateMax()).isEqualTo(option.getLendRateMax());
        assertThat(dto.getLendRateAvg()).isEqualTo(option.getLendRateAvg());

        // 유형 정보
        assertThat(dto.getCrdtLendRateType()).isEqualTo(option.getCrdtLendRateType());
        assertThat(dto.getCrdtLendRateTypeNm()).isEqualTo(option.getCrdtLendRateTypeNm());

        // 등급별 금리
        assertThat(dto.getCrdtGrad1()).isEqualTo(option.getCrdtGrad1());
        assertThat(dto.getCrdtGrad4()).isEqualTo(option.getCrdtGrad4());
        assertThat(dto.getCrdtGrad5()).isEqualTo(option.getCrdtGrad5());
        assertThat(dto.getCrdtGrad6()).isEqualTo(option.getCrdtGrad6());
        assertThat(dto.getCrdtGrad10()).isEqualTo(option.getCrdtGrad10());
        assertThat(dto.getCrdtGrad11()).isEqualTo(option.getCrdtGrad11());
        assertThat(dto.getCrdtGrad12()).isEqualTo(option.getCrdtGrad12());
        assertThat(dto.getCrdtGrad13()).isEqualTo(option.getCrdtGrad13());
        assertThat(dto.getCrdtGradAvg()).isEqualTo(option.getCrdtGradAvg());

        // 추천용 필드는 fromEntity에서 null로 채우도록 설계됨
        assertThat(dto.getScore()).isNull();
        assertThat(dto.getReason()).isNull();
    }

    // -------------------------------------------------
    // [3] FinanceProductsResponse.fromEntity 테스트
    // -------------------------------------------------
    @Test
    @DisplayName("FinanceProductsResponse.fromEntity - Products와 FinanceProducts를 함께 매핑")
    void financeProductsResponse_fromEntity_withProduct() {
        // given
        Products product = createProducts();
        FinanceProducts fp = createFinanceProducts(product, FinanceProductType.DEPOSIT);

        // when
        FinanceProductsResponse dto = FinanceProductsResponse.fromEntity(fp);

        // then
        assertThat(dto.getId()).isEqualTo(fp.getId());
        assertThat(dto.getProductId()).isEqualTo(product.getId());
        assertThat(dto.getProductName()).isEqualTo(product.getName());
        assertThat(dto.getProvider()).isEqualTo(product.getProvider());
        assertThat(dto.getDetailUrl()).isEqualTo(product.getDetailUrl());

        assertThat(dto.getFinCoNo()).isEqualTo(fp.getFinCoNo());
        assertThat(dto.getProductType()).isEqualTo(FinanceProductType.DEPOSIT);
        assertThat(dto.getInterestRate()).isEqualTo(fp.getInterestRate());
        assertThat(dto.getMinDeposit()).isEqualTo(fp.getMinDeposit());
        assertThat(dto.getJoinCondition()).isEqualTo(fp.getJoinCondition());

        // 추천용 필드는 null로 초기화
        assertThat(dto.getScore()).isNull();
        assertThat(dto.getReason()).isNull();
    }

    @Test
    @DisplayName("FinanceProductsResponse.fromEntity - Product가 null이어도 NPE 없이 null 매핑")
    void financeProductsResponse_fromEntity_withoutProduct() {
        // given
        FinanceProducts fp = createFinanceProducts(null, FinanceProductType.SAVING);

        // when
        FinanceProductsResponse dto = FinanceProductsResponse.fromEntity(fp);

        // then
        assertThat(dto.getProductId()).isNull();
        assertThat(dto.getProductName()).isNull();
        assertThat(dto.getProvider()).isNull();
        assertThat(dto.getDetailUrl()).isNull();

        // FinanceProducts 자체 필드는 그대로 매핑
        assertThat(dto.getFinCoNo()).isEqualTo(fp.getFinCoNo());
        assertThat(dto.getProductType()).isEqualTo(FinanceProductType.SAVING);
    }

    // -------------------------------------------------
    // [4] RentLoanResponse.fromEntity 테스트
    // -------------------------------------------------
    @Test
    @DisplayName("RentLoanResponse.fromEntity - 전세자금대출 옵션 매핑")
    void rentLoanResponse_fromEntity() {
        // given
        Products product = createProducts();
        FinanceProducts fp = createFinanceProducts(product, FinanceProductType.RENT_HOUSE_LOAN);
        FinanceLoanOption option = createFinanceLoanOption(fp);

        // when
        RentLoanResponse dto = RentLoanResponse.fromEntity(option);

        // then
        assertThat(dto.getProductId()).isEqualTo(product.getId());
        assertThat(dto.getProductName()).isEqualTo(product.getName());
        assertThat(dto.getCompanyName()).isEqualTo(product.getProvider());
        assertThat(dto.getProductType()).isEqualTo(FinanceProductType.RENT_HOUSE_LOAN);

        assertThat(dto.getLendRateMin()).isEqualTo(option.getLendRateMin());
        assertThat(dto.getLendRateMax()).isEqualTo(option.getLendRateMax());
        assertThat(dto.getLendRateAvg()).isEqualTo(option.getLendRateAvg());
        assertThat(dto.getLendTypeName()).isEqualTo(option.getLendTypeName());
        assertThat(dto.getRpayTypeName()).isEqualTo(option.getRpayTypeName());
    }
}
