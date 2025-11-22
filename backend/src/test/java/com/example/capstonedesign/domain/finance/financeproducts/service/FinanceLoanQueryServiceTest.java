package com.example.capstonedesign.domain.finance.financeproducts.service;

import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinanceLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceLoanOption;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceLoanOptionRepository;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceProductsRepository;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceLoanQueryServiceTest {

    @Mock
    UsersRepository usersRepository;

    @Mock
    FinanceProductsRepository financeProductsRepository;

    @Mock
    FinanceLoanOptionRepository loanOptionRepository;

    @InjectMocks
    FinanceLoanQueryService service;

    @Test
    @DisplayName("사용자 맞춤 대출 추천 로직 - 청년·중저소득·전세대출 케이스")
    void recommendLoansForUser_logic() {
        // given: 사용자 (30세, 중위소득 150% 이하)
        Users user = new Users();
        user.setId(1);
        user.setAge(30);
        user.setIncome_band("중위소득 150% 이하");

        when(usersRepository.findById(1))
                .thenReturn(Optional.of(user));

        // 대출 상품 (전세자금대출)
        Products p = Products.builder()
                .id(100)
                .name("테스트 전세자금대출")
                .provider("테스트은행")
                .detailUrl("https://test-bank.com")
                .build();

        FinanceProducts loanProduct = FinanceProducts.builder()
                .id(10)
                .product(p)
                .finCoNo("001")
                .productType(FinanceProductType.RENT_HOUSE_LOAN)
                .build();

        when(financeProductsRepository.findByProductTypeIn(anyList()))
                .thenReturn(List.of(loanProduct));

        // 옵션 (평균 금리 3.50%)
        FinanceLoanOption option = FinanceLoanOption.builder()
                .financeProduct(loanProduct)
                .lendRateAvg(new BigDecimal("3.50"))
                .build();

        when(loanOptionRepository.findByFinanceProduct(loanProduct))
                .thenReturn(List.of(option));

        // when
        List<FinanceLoanResponse> result = service.recommendLoansForUser(1);

        // then
        assertEquals(1, result.size(), "추천 결과는 1건이어야 한다.");

        FinanceLoanResponse r = result.get(0);
        assertEquals(100, r.getProductId());
        assertEquals("테스트 전세자금대출", r.getProductName());
        assertEquals(FinanceProductType.RENT_HOUSE_LOAN, r.getProductType());
        assertNotNull(r.getScore());
        assertTrue(r.getScore() > 0);

        // 점수 예상값 근사 검증 (rate=3.5, age=30, income=150% 이하, type=RENT_HOUSE_LOAN)
        double expectedScore = (3.5 * 0.6) * 0.8 * 0.85 * 0.8; // ≒ 1.1424
        assertEquals(expectedScore, r.getScore(), 1e-6);

        // 추천 사유 문자열에 핵심 키워드 포함 여부 확인
        assertTrue(r.getReason().contains("평균 금리"), "reason에 평균 금리 설명이 포함되어야 한다.");
        assertTrue(r.getReason().contains("청년층") || r.getReason().contains("청년"), "reason에 청년층 관련 설명이 포함되어야 한다.");
    }
}