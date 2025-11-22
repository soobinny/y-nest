package com.example.capstonedesign.domain.finance.financeproducts.controller;

import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinanceLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.MortgageLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.RentLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.LoanProductType;
import com.example.capstonedesign.domain.finance.financeproducts.service.FinanceLoanQueryService;
import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinanceLoansController.class)
@AutoConfigureMockMvc(addFilters = false)
class FinanceLoansControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    FinanceLoanQueryService loanQueryService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("주택담보대출 옵션 조회 - 성공")
    void getMortgageLoans_success() throws Exception {
        // given
        MortgageLoanResponse res = MortgageLoanResponse.builder()
                .productId(10)
                .productName("내집마련 주택담보대출")
                .companyName("A은행")
                .productType(FinanceProductType.MORTGAGE_LOAN)
                .lendRateMin(new BigDecimal("3.00"))
                .lendRateMax(new BigDecimal("4.00"))
                .lendRateAvg(new BigDecimal("3.50"))
                .lendTypeName("고정금리")
                .rpayTypeName("원리금균등상환")
                .mrtgTypeName("아파트담보")
                .build();

        when(loanQueryService.getMortgageLoans())
                .thenReturn(List.of(res));

        // when & then
        mvc.perform(get("/api/finance/loans/options/type/{loanType}",
                        LoanProductType.MORTGAGE_LOAN.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productName").value("내집마련 주택담보대출"))
                .andExpect(jsonPath("$[0].companyName").value("A은행"))
                .andExpect(jsonPath("$[0].productType").value("MORTGAGE_LOAN"))
                .andExpect(jsonPath("$[0].lendRateAvg").value(3.50));

        verify(loanQueryService).getMortgageLoans();
    }

    @Test
    @DisplayName("전세자금대출 옵션 조회 - 성공")
    void getRentLoans_success() throws Exception {
        // given
        RentLoanResponse res = RentLoanResponse.builder()
                .productId(20)
                .productName("청년 전세자금대출")
                .companyName("B은행")
                .productType(FinanceProductType.RENT_HOUSE_LOAN)
                .lendRateMin(new BigDecimal("2.80"))
                .lendRateMax(new BigDecimal("3.60"))
                .lendRateAvg(new BigDecimal("3.20"))
                .lendTypeName("변동금리")
                .rpayTypeName("원리금균등상환")
                .build();

        when(loanQueryService.getRentLoans())
                .thenReturn(List.of(res));

        // when & then
        mvc.perform(get("/api/finance/loans/options/type/{loanType}",
                        LoanProductType.RENT_HOUSE_LOAN.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productName").value("청년 전세자금대출"))
                .andExpect(jsonPath("$[0].productType").value("RENT_HOUSE_LOAN"))
                .andExpect(jsonPath("$[0].lendRateAvg").value(3.20));

        verify(loanQueryService).getRentLoans();
    }

    @Test
    @DisplayName("개인신용대출 옵션 조회 - 성공")
    void getCreditLoans_success() throws Exception {
        // given
        FinanceLoanResponse res = FinanceLoanResponse.builder()
                .productId(30)
                .productName("직장인 신용대출")
                .companyName("C은행")
                .productType(FinanceProductType.CREDIT_LOAN)
                .crdtLendRateType("A")
                .crdtLendRateTypeNm("고정금리")
                .crdtGradAvg(new BigDecimal("4.20"))
                .build();

        when(loanQueryService.getCreditLoans())
                .thenReturn(List.of(res));

        // when & then
        mvc.perform(get("/api/finance/loans/options/type/{loanType}",
                        LoanProductType.CREDIT_LOAN.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productName").value("직장인 신용대출"))
                .andExpect(jsonPath("$[0].productType").value("CREDIT_LOAN"))
                .andExpect(jsonPath("$[0].crdtGradAvg").value(4.20));

        verify(loanQueryService).getCreditLoans();
    }

    @Test
    @DisplayName("사용자 맞춤 대출 추천 - 성공")
    void recommendLoansForUser_success() throws Exception {
        // given
        FinanceLoanResponse res = FinanceLoanResponse.builder()
                .productId(40)
                .productName("맞춤형 청년 전세대출")
                .companyName("D은행")
                .productType(FinanceProductType.RENT_HOUSE_LOAN)
                .lendRateAvg(new BigDecimal("2.90"))
                .score(1.23)
                .reason("평균 금리 2.90%, 청년층 우대, 전세자금대출 중심")
                .build();

        when(loanQueryService.recommendLoansForUser(eq(1)))
                .thenReturn(List.of(res));

        // when & then
        mvc.perform(get("/api/finance/loans/options/recommend/{userId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productName").value("맞춤형 청년 전세대출"))
                .andExpect(jsonPath("$[0].productType").value("RENT_HOUSE_LOAN"))
                .andExpect(jsonPath("$[0].score").value(1.23));

        verify(loanQueryService).recommendLoansForUser(1);
    }
}
