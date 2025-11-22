package com.example.capstonedesign.domain.finance.financeproducts.service;

import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinanceProductsResponse;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceProductsRepository;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.users.entity.UserRole;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * FinanceProductRecommendationService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class FinanceProductRecommendationServiceTest {

    @Mock
    UsersRepository usersRepository;

    @Mock
    FinanceProductsRepository financeProductsRepository;

    @InjectMocks
    FinanceProductRecommendationService recommendationService;

    private Users youngLowIncomeUser;

    @BeforeEach
    void setUp() {
        youngLowIncomeUser = Users.builder()
                .id(1)
                .email("user@example.com")
                .password("Password1!")
                .name("테스터")
                .age(26)                           // 청년
                .income_band("중위소득100%이하")    // 저소득
                .region("서울")
                .is_homeless(false)
                .birthdate(LocalDate.of(1999, 1, 1))
                .role(UserRole.USER)
                .created_at(Instant.now())
                .updated_at(Instant.now())
                .notificationEnabled(true)
                .build();
    }

    @Test
    @DisplayName("예금 추천 - 청년 · 저소득 사용자는 고금리 · 저예치금 상품이 상위로 온다")
    void recommendDepositOrSaving_young_lowIncome_sortedByScore() {
        // given
        when(usersRepository.findById(1))
                .thenReturn(Optional.of(youngLowIncomeUser));

        Products p1 = Products.builder()
                .id(100)
                .name("고금리 소액 예금")
                .provider("A은행")
                .detailUrl("https://a.com")
                .build();

        Products p2 = Products.builder()
                .id(101)
                .name("저금리 고액 예금")
                .provider("B은행")
                .detailUrl("https://b.com")
                .build();

        FinanceProducts fp1 = FinanceProducts.builder()
                .id(1)
                .product(p1)
                .finCoNo("001")
                .productType(FinanceProductType.DEPOSIT)
                .interestRate(new BigDecimal("4.50")) // 더 높은 금리
                .minDeposit(100_000)                  // 더 낮은 예치금
                .joinCondition("청년 전용")
                .build();

        FinanceProducts fp2 = FinanceProducts.builder()
                .id(2)
                .product(p2)
                .finCoNo("002")
                .productType(FinanceProductType.DEPOSIT)
                .interestRate(new BigDecimal("2.00"))
                .minDeposit(5_000_000)
                .joinCondition("일반 고객")
                .build();

        when(financeProductsRepository.findByProductType(FinanceProductType.DEPOSIT))
                .thenReturn(List.of(fp1, fp2));

        // when
        List<FinanceProductsResponse> result =
                recommendationService.recommendDepositOrSaving(1, FinanceProductType.DEPOSIT);

        // then
        assertEquals(2, result.size());

        FinanceProductsResponse first = result.get(0);
        FinanceProductsResponse second = result.get(1);

        // 점수가 낮을수록 우선 → fp1이 먼저 와야 함
        assertEquals("고금리 소액 예금", first.getProductName());
        assertTrue(first.getScore() <= second.getScore());

        // 추천 사유에 청년/저소득 관련 문구가 포함되는지 확인
        String reason = first.getReason();
        assertTrue(reason.contains("금리"));
        assertTrue(reason.contains("청년층 우대"), "청년층 우대 문구가 포함되어야 합니다.");
        assertTrue(reason.contains("저소득층") || reason.contains("저소득층 혜택"),
                "저소득 관련 문구가 포함되어야 합니다.");
    }

    @Test
    @DisplayName("소득 구간에 공백이 섞여 있어도 정상 동작한다")
    void recommend_withIncomeBandHavingSpaces() {
        // given: 소득 구간에 공백 포함
        youngLowIncomeUser.setIncome_band("중위소득 100% 이하");

        when(usersRepository.findById(1))
                .thenReturn(Optional.of(youngLowIncomeUser));

        FinanceProducts fp = FinanceProducts.builder()
                .id(1)
                .product(Products.builder()
                        .id(100)
                        .name("테스트 예금")
                        .provider("C은행")
                        .build())
                .finCoNo("003")
                .productType(FinanceProductType.DEPOSIT)
                .interestRate(new BigDecimal("3.00"))
                .minDeposit(500_000)
                .joinCondition("테스트 조건")
                .build();

        when(financeProductsRepository.findByProductType(FinanceProductType.DEPOSIT))
                .thenReturn(List.of(fp));

        // when
        List<FinanceProductsResponse> result =
                recommendationService.recommendDepositOrSaving(1, FinanceProductType.DEPOSIT);

        // then
        assertEquals(1, result.size());
        assertEquals("테스트 예금", result.get(0).getProductName());
        // 예외 없이 reason이 생성되었는지만 확인
        assertNotNull(result.get(0).getReason());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 요청 시 예외 발생")
    void recommend_unknownUser_throwsException() {
        // given
        when(usersRepository.findById(any()))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> recommendationService.recommendDepositOrSaving(999, FinanceProductType.DEPOSIT));
    }
}
