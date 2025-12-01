package com.example.capstonedesign.domain.finance.financeproducts.controller;

import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinanceProductsResponse;
import com.example.capstonedesign.domain.finance.financeproducts.entity.DSProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceProductsRepository;
import com.example.capstonedesign.domain.finance.financeproducts.service.FinanceProductRecommendationService;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FinanceProductQueryController WebMvc 테스트
 */
@WebMvcTest(FinanceProductQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
class FinanceProductQueryControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    FinanceProductsRepository repo;

    @MockitoBean
    FinanceProductRecommendationService recommendService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    // ----------------------------------------------------------
    // 1. /api/finance/products 목록 조회
    // ----------------------------------------------------------
    @SuppressWarnings("unchecked") // any(Specification.class)에서 나는 unchecked 경고 제거용
    @Test
    @DisplayName("금융 상품 목록 조회 - 기본 조회 성공")
    void list_success() throws Exception {
        // given
        Products p = Products.builder()
                .id(10)
                .name("청년 우대 예금")
                .provider("A은행")
                .detailUrl("https://a.com")
                .build();

        FinanceProducts fp = FinanceProducts.builder()
                .id(1)
                .product(p)
                .finCoNo("001")
                .productType(FinanceProductType.DEPOSIT)
                .interestRate(new BigDecimal("3.50"))
                .minDeposit(100_000)
                .joinCondition("만 19~34세 청년 전용")
                .build();

        Page<FinanceProducts> page =
                new PageImpl<>(List.of(fp), PageRequest.of(0, 10), 1);

        when(repo.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(page);

        // when & then
        mvc.perform(get("/api/finance/products")
                        .param("productType", "DEPOSIT")
                        .param("minRate", "3.0")
                        .param("maxRate", "5.0")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "interestRate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].productName").value("청년 우대 예금"))
                .andExpect(jsonPath("$.content[0].provider").value("A은행"))
                .andExpect(jsonPath("$.content[0].productType").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[0].interestRate").value(3.50));

        // sanitizePageable 이 동작해 repo.findAll 이 한 번은 호출되었는지만 검증
        verify(repo).findAll(any(Specification.class), any(Pageable.class));
    }

    // ----------------------------------------------------------
    // 2. /api/finance/products/recommend/{userId}
    // ----------------------------------------------------------
    @Test
    @DisplayName("추천 API - DSProductType → FinanceProductType 매핑 및 응답 구조 검증")
    void recommendForUser_success() throws Exception {
        // given
        FinanceProductsResponse res = FinanceProductsResponse.builder()
                .id(1)
                .productId(10)
                .productName("청년 적금")
                .provider("B은행")
                .finCoNo("002")
                .productType(FinanceProductType.SAVING)
                .interestRate(new BigDecimal("4.00"))
                .minDeposit(50_000)
                .joinCondition("청년 전용 적금")
                .score(1.23)
                .reason("금리 4.00%, 청년층 우대 가능, 적금 상품")
                .build();

        when(recommendService.recommendDepositOrSaving(1, FinanceProductType.SAVING))
                .thenReturn(List.of(res));

        // when & then
        mvc.perform(get("/api/finance/products/recommend/{userId}", 1)
                        .param("type", DSProductType.SAVING.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("청년 적금"))
                .andExpect(jsonPath("$[0].productType").value("SAVING"))
                .andExpect(jsonPath("$[0].score").value(1.23));

        // DSProductType.SAVING 이 FinanceProductType.SAVING 으로 매핑되어 서비스 호출되었는지 확인
        verify(recommendService)
                .recommendDepositOrSaving(1, FinanceProductType.SAVING);
    }
}
