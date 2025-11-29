package com.example.capstonedesign.application.ingest.Finance;

import com.example.capstonedesign.domain.finance.financecompanies.entity.FinanceCompanies;
import com.example.capstonedesign.domain.finance.financecompanies.repository.FinanceCompaniesRepository;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinlifeCreditLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinlifeMortgageLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinlifeRentLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceLoanOption;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceLoanOptionRepository;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceProductsRepository;
import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import com.example.capstonedesign.infra.finlife.FinlifeClient;
import com.example.capstonedesign.infra.finlife.dto.FinlifeCompanySearchResponse;
import com.example.capstonedesign.infra.finlife.dto.FinlifeProductResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class
FinlifeIngestServiceTest {

    @Mock
    private FinlifeClient client;

    @Mock
    private FinanceCompaniesRepository companiesRepository;

    @Mock
    private FinanceProductsRepository financeProductsRepository;

    @Mock
    private ProductsRepository productsRepository;

    @Mock
    private FinanceLoanOptionRepository loanOptionRepository;

    @InjectMocks
    private FinlifeIngestService service;

    // ==========================
    // hasInitialData()
    // ==========================

    @Test
    void hasInitialData_returnsFalse_whenCountIsZero() {
        when(companiesRepository.count()).thenReturn(0L);

        boolean result = service.hasInitialData();

        assertThat(result).isFalse();
        verify(companiesRepository).count();
    }

    @Test
    void hasInitialData_returnsTrue_whenCountGreaterThanZero() {
        when(companiesRepository.count()).thenReturn(3L);

        boolean result = service.hasInitialData();

        assertThat(result).isTrue();
        verify(companiesRepository).count();
    }

    // ==========================
    // ensureCompany()
    // ==========================

    @Test
    void ensureCompany_createsNewCompany_whenNotExists() throws Exception {
        // given
        String rawCoNo = "\u00A0 020000 ";  // NBSP + 공백 섞인 코드
        String name = "국민은행";
        String homepage = "https://kb.com";
        String contact = "1588-0000";

        when(companiesRepository.findByFinCoNo("020000"))
                .thenReturn(Optional.empty());

        Method m = FinlifeIngestService.class
                .getDeclaredMethod("ensureCompany", String.class, String.class, String.class, String.class);
        m.setAccessible(true);

        // when
        m.invoke(service, rawCoNo, name, homepage, contact);

        // then
        ArgumentCaptor<FinanceCompanies> captor = ArgumentCaptor.forClass(FinanceCompanies.class);
        verify(companiesRepository).save(captor.capture());

        FinanceCompanies saved = captor.getValue();
        assertThat(saved.getFinCoNo()).isEqualTo("020000");
        assertThat(saved.getName()).isEqualTo("국민은행");
        assertThat(saved.getHomepage()).isEqualTo("https://kb.com");
        assertThat(saved.getContact()).isEqualTo("1588-0000");
    }

    @Test
    void ensureCompany_updatesExistingCompany_whenExists() throws Exception {
        // given
        FinanceCompanies existing = FinanceCompanies.builder()
                .finCoNo("020000")
                .name("OLD")
                .homepage("old-url")
                .contact("old-contact")
                .build();

        when(companiesRepository.findByFinCoNo("020000"))
                .thenReturn(Optional.of(existing));

        Method m = FinlifeIngestService.class
                .getDeclaredMethod("ensureCompany", String.class, String.class, String.class, String.class);
        m.setAccessible(true);

        // when
        m.invoke(service, "020000", "새이름", "https://new-url", "02-0000-0000");

        // then
        ArgumentCaptor<FinanceCompanies> captor = ArgumentCaptor.forClass(FinanceCompanies.class);
        verify(companiesRepository).save(captor.capture());

        FinanceCompanies saved = captor.getValue();
        // 같은 엔티티 인스턴스가 수정되어 저장되는지
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getName()).isEqualTo("새이름");
        assertThat(saved.getHomepage()).isEqualTo("https://new-url");
        assertThat(saved.getContact()).isEqualTo("02-0000-0000");
    }

    // ==========================
    // saveOrUpdateLoanOption()
    // ==========================

    @Test
    void saveOrUpdateLoanOption_whenNoExistingOption_savesNewOptionWithoutPrevRates() throws Exception {
        FinanceProducts financeProduct = FinanceProducts.builder().id(1).build();

        FinanceLoanOption newOpt = FinanceLoanOption.builder()
                .financeProduct(financeProduct)
                .lendRateMin(new BigDecimal("3.00"))
                .lendRateMax(new BigDecimal("3.50"))
                .lendRateAvg(new BigDecimal("3.20"))
                .rpayTypeName("원리금균등분할상환")
                .lendTypeName("고정금리")
                .mrtgTypeName("아파트")
                .build();

        when(loanOptionRepository
                .findTopByFinanceProductAndRpayTypeNameAndLendTypeNameAndMrtgTypeName(
                        financeProduct, "원리금균등분할상환", "고정금리", "아파트"))
                .thenReturn(Optional.empty());

        Method m = FinlifeIngestService.class
                .getDeclaredMethod("saveOrUpdateLoanOption", FinanceLoanOption.class);
        m.setAccessible(true);

        m.invoke(service, newOpt);

        ArgumentCaptor<FinanceLoanOption> captor =
                ArgumentCaptor.forClass(FinanceLoanOption.class);
        verify(loanOptionRepository).save(captor.capture());

        FinanceLoanOption saved = captor.getValue();
        assertThat(saved.getLendRateMin()).isEqualByComparingTo("3.00");
        assertThat(saved.getLendRateMax()).isEqualByComparingTo("3.50");
        assertThat(saved.getLendRateAvg()).isEqualByComparingTo("3.20");
        // prev 값은 null 이어야 함
        assertThat(saved.getPrevLendRateMin()).isNull();
        assertThat(saved.getPrevLendRateMax()).isNull();
        assertThat(saved.getPrevLendRateAvg()).isNull();
    }

    @Test
    void saveOrUpdateLoanOption_whenExistingOption_updatesRatesAndBacksUpPrevRates() throws Exception {
        FinanceProducts financeProduct = FinanceProducts.builder().id(1).build();

        FinanceLoanOption existing = FinanceLoanOption.builder()
                .financeProduct(financeProduct)
                .lendRateMin(new BigDecimal("3.00"))
                .lendRateMax(new BigDecimal("3.40"))
                .lendRateAvg(new BigDecimal("3.10"))
                .rpayTypeName("원리금균등분할상환")
                .lendTypeName("고정금리")
                .mrtgTypeName("아파트")
                .build();

        FinanceLoanOption newOpt = FinanceLoanOption.builder()
                .financeProduct(financeProduct)
                .lendRateMin(new BigDecimal("3.20"))
                .lendRateMax(new BigDecimal("3.80"))
                .lendRateAvg(new BigDecimal("3.50"))
                .rpayTypeName("원리금균등분할상환")
                .lendTypeName("고정금리")
                .mrtgTypeName("아파트")
                .build();

        when(loanOptionRepository
                .findTopByFinanceProductAndRpayTypeNameAndLendTypeNameAndMrtgTypeName(
                        financeProduct, "원리금균등분할상환", "고정금리", "아파트"))
                .thenReturn(Optional.of(existing));

        Method m = FinlifeIngestService.class
                .getDeclaredMethod("saveOrUpdateLoanOption", FinanceLoanOption.class);
        m.setAccessible(true);

        m.invoke(service, newOpt);

        ArgumentCaptor<FinanceLoanOption> captor =
                ArgumentCaptor.forClass(FinanceLoanOption.class);
        verify(loanOptionRepository).save(captor.capture());

        FinanceLoanOption saved = captor.getValue();

        // prev* 에 이전 값 백업
        assertThat(saved.getPrevLendRateMin()).isEqualByComparingTo("3.00");
        assertThat(saved.getPrevLendRateMax()).isEqualByComparingTo("3.40");
        assertThat(saved.getPrevLendRateAvg()).isEqualByComparingTo("3.10");

        // 현재 금리는 새 값
        assertThat(saved.getLendRateMin()).isEqualByComparingTo("3.20");
        assertThat(saved.getLendRateMax()).isEqualByComparingTo("3.80");
        assertThat(saved.getLendRateAvg()).isEqualByComparingTo("3.50");

        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    // ==========================
    // upsertProduct()
    // ==========================

    @Test
    void upsertProduct_returnsExisting_whenFound() throws Exception {
        Products existing = Products.builder()
                .type(ProductType.FINANCE)
                .name("청년 희망 적금")
                .provider("OO은행")
                .build();

        when(productsRepository.findByTypeAndNameAndProvider(
                ProductType.FINANCE, "청년 희망 적금", "OO은행"))
                .thenReturn(Optional.of(existing));

        Method m = FinlifeIngestService.class
                .getDeclaredMethod("upsertProduct", String.class, String.class, String.class);
        m.setAccessible(true);

        Products result = (Products) m.invoke(service,
                "청년 희망 적금", "OO은행", "020000");

        assertThat(result).isSameAs(existing);
        verify(productsRepository, never()).save(any());
    }

    @Test
    void upsertProduct_savesNew_whenNotFound() throws Exception {
        when(productsRepository.findByTypeAndNameAndProvider(
                ProductType.FINANCE, "청년 희망 적금", "OO은행"))
                .thenReturn(Optional.empty());

        Products saved = Products.builder()
                .type(ProductType.FINANCE)
                .name("청년 희망 적금")
                .provider("OO은행")
                .build();

        when(productsRepository.save(any(Products.class)))
                .thenReturn(saved);

        Method m = FinlifeIngestService.class
                .getDeclaredMethod("upsertProduct", String.class, String.class, String.class);
        m.setAccessible(true);

        Products result = (Products) m.invoke(service,
                "청년 희망 적금", "OO은행", "020000");

        assertThat(result).isSameAs(saved);
        verify(productsRepository).save(any(Products.class));
    }

    // ==========================
    // upsertFinanceProduct()
    // ==========================

    @Test
    void upsertFinanceProduct_returnsExisting_whenFound() throws Exception {
        Products prod = Products.builder()
                .type(ProductType.FINANCE)
                .name("청년 희망 적금")
                .provider("OO은행")
                .build();

        FinanceProducts fp = FinanceProducts.builder()
                .product(prod)
                .finCoNo("020000")
                .productType(FinanceProductType.DEPOSIT)
                .build();

        when(financeProductsRepository.findByProductAndFinCoNo(prod, "020000"))
                .thenReturn(Optional.of(fp));

        Method m = FinlifeIngestService.class
                .getDeclaredMethod("upsertFinanceProduct", Products.class, String.class, FinanceProductType.class);
        m.setAccessible(true);

        FinanceProducts result = (FinanceProducts) m.invoke(service,
                prod, "020000", FinanceProductType.DEPOSIT);

        assertThat(result).isSameAs(fp);
        verify(financeProductsRepository, never()).save(any());
    }

    @Test
    void upsertFinanceProduct_savesNew_whenNotFound() throws Exception {
        Products prod = Products.builder()
                .type(ProductType.FINANCE)
                .name("청년 희망 적금")
                .provider("OO은행")
                .build();

        when(financeProductsRepository.findByProductAndFinCoNo(prod, "020000"))
                .thenReturn(Optional.empty());

        FinanceProducts saved = FinanceProducts.builder()
                .product(prod)
                .finCoNo("020000")
                .productType(FinanceProductType.DEPOSIT)
                .build();

        when(financeProductsRepository.save(any(FinanceProducts.class)))
                .thenReturn(saved);

        Method m = FinlifeIngestService.class
                .getDeclaredMethod("upsertFinanceProduct", Products.class, String.class, FinanceProductType.class);
        m.setAccessible(true);

        FinanceProducts result = (FinanceProducts) m.invoke(service,
                prod, "020000", FinanceProductType.DEPOSIT);

        assertThat(result).isSameAs(saved);
        verify(financeProductsRepository).save(any(FinanceProducts.class));
    }

    // ==========================
    // extractLendRate()
    // ==========================

    /** 금리 옵션용 간단한 스텁 클래스 */
    static class TestLoanOption {
        private final String finPrdtCd;
        private final String finCoNo;
        private final BigDecimal lendRateMin;
        private final BigDecimal lendRateMax;
        private final BigDecimal lendRateAvg;

        TestLoanOption(String finPrdtCd, String finCoNo,
                       BigDecimal lendRateMin, BigDecimal lendRateMax, BigDecimal lendRateAvg) {
            this.finPrdtCd = finPrdtCd;
            this.finCoNo = finCoNo;
            this.lendRateMin = lendRateMin;
            this.lendRateMax = lendRateMax;
            this.lendRateAvg = lendRateAvg;
        }

        public String getFinPrdtCd() { return finPrdtCd; }
        public String getFinCoNo() { return finCoNo; }
        public BigDecimal getLendRateMin() { return lendRateMin; }
        public BigDecimal getLendRateMax() { return lendRateMax; }
        public BigDecimal getLendRateAvg() { return lendRateAvg; }
    }

    @Test
    void extractLendRate_picksMaxOfAvailableRates_forMatchingOptions() throws Exception {
        TestLoanOption opt1 = new TestLoanOption(
                "PRD1", "020000",
                new BigDecimal("3.00"),
                new BigDecimal("3.40"),
                null  // avg 없음 → max 사용
        );
        TestLoanOption opt2 = new TestLoanOption(
                "PRD1", "020000",
                new BigDecimal("3.10"),
                null,
                new BigDecimal("3.60") // avg 있음 → 이 값 사용
        );
        TestLoanOption optOther = new TestLoanOption(
                "PRD2", "020000",
                new BigDecimal("10.00"),
                new BigDecimal("12.00"),
                new BigDecimal("11.00")
        );

        List<TestLoanOption> options = List.of(opt1, opt2, optOther);

        Method m = FinlifeIngestService.class
                .getDeclaredMethod("extractLendRate", List.class, String.class, String.class);
        m.setAccessible(true);

        BigDecimal result = (BigDecimal) m.invoke(service, options, "PRD1", "020000");

        // PRD1 + 020000 에 해당하는 것들 중
        // opt1: max=3.40, opt2: avg=3.60 → 최종 3.60 이어야 함
        assertThat(result).isEqualByComparingTo("3.60");
    }

    @Test
    void extractLendRate_returnsNull_whenNoMatchingOptions() throws Exception {
        TestLoanOption opt = new TestLoanOption(
                "PRD_X", "999999",
                new BigDecimal("5.00"),
                new BigDecimal("5.50"),
                new BigDecimal("5.25")
        );

        List<TestLoanOption> options = List.of(opt);

        Method m = FinlifeIngestService.class
                .getDeclaredMethod("extractLendRate", List.class, String.class, String.class);
        m.setAccessible(true);

        BigDecimal result = (BigDecimal) m.invoke(service, options, "PRD1", "020000");

        assertThat(result).isNull();
    }

    // ==========================
    // 문자열 유틸 (normalizeCoNo / codeOrName / safeLine) - 필요하면 그대로 유지
    // ==========================

    @Test
    void normalizeCoNo_removesNonBreakingSpaceAndTrims() throws Exception {
        Method m = FinlifeIngestService.class
                .getDeclaredMethod("normalizeCoNo", String.class);
        m.setAccessible(true);

        String raw = "\u00A0 020000 \u00A0";
        String normalized = (String) m.invoke(service, raw);

        assertThat(normalized).isEqualTo("020000");
    }

    @Test
    void codeOrName_prefersCode_whenCodeIsNotBlank() throws Exception {
        Method m = FinlifeIngestService.class
                .getDeclaredMethod("codeOrName", String.class, String.class);
        m.setAccessible(true);

        String result1 = (String) m.invoke(null, "020000", "국민은행");
        String result2 = (String) m.invoke(null, "   ", "국민은행");
        String result3 = (String) m.invoke(null, null, "국민은행");

        assertThat(result1).isEqualTo("020000");
        assertThat(result2).isEqualTo("국민은행");
        assertThat(result3).isEqualTo("국민은행");
    }

    @Test
    void safeLine_returnsEmptyWhenValueBlank_otherwiseConcatLabelAndValue() throws Exception {
        Method m = FinlifeIngestService.class
                .getDeclaredMethod("safeLine", String.class, String.class);
        m.setAccessible(true);

        String line1 = (String) m.invoke(null, "가입 방법: ", "인터넷 / 모바일");
        String line2 = (String) m.invoke(null, "가입 방법: ", "   ");

        assertThat(line1).isEqualTo("가입 방법: 인터넷 / 모바일");
        assertThat(line2).isEqualTo("");
    }

    @Test
    void syncCompanies_returnsZero_whenResultIsNull() {
        // given: 모든 그룹/페이지에서 getCompanies(...) 가 result=null 인 응답을 리턴
        FinlifeCompanySearchResponse res = mock(FinlifeCompanySearchResponse.class);
        when(res.getResult()).thenReturn(null);
        when(client.getCompanies(anyString(), anyInt())).thenReturn(res);

        // when
        int saved = service.syncCompanies(3);

        // then
        assertThat(saved).isEqualTo(0);
        // list 가 null 이라 내부 for 루프는 안 돌고, save 도 안 호출돼야 함
        verify(companiesRepository, never()).save(any());
    }

    @Test
    void syncDepositAndSaving_returnsZero_whenNoBaseListInResponses() {
        // given: 예금/적금 둘 다 result=null 인 응답만 내려오는 상황
        FinlifeProductResponse depositRes = mock(FinlifeProductResponse.class);
        FinlifeProductResponse savingRes = mock(FinlifeProductResponse.class);

        when(depositRes.getResult()).thenReturn(null);
        when(savingRes.getResult()).thenReturn(null);

        when(client.getDepositProducts(anyString(), anyInt())).thenReturn(depositRes);
        when(client.getSavingProducts(anyString(), anyInt())).thenReturn(savingRes);

        // when
        int saved = service.syncDepositAndSaving(2);

        // then
        assertThat(saved).isEqualTo(0);
        // bases 가 null 이라 products / finance_products upsert 로직이 안 타야 함
        verify(productsRepository, never()).save(any());
        verify(financeProductsRepository, never()).save(any());
    }

    @Test
    void syncLoans_returnsZero_whenLoanResponsesHaveNoBaseList() {
        // given: 주택/전세/신용 대출 응답에서 result=null
        FinlifeMortgageLoanResponse mortRes = mock(FinlifeMortgageLoanResponse.class);
        FinlifeRentLoanResponse rentRes = mock(FinlifeRentLoanResponse.class);
        FinlifeCreditLoanResponse creditRes = mock(FinlifeCreditLoanResponse.class);

        when(mortRes.getResult()).thenReturn(null);
        when(rentRes.getResult()).thenReturn(null);
        when(creditRes.getResult()).thenReturn(null);

        when(client.getMortgageLoanProducts(anyString(), anyInt())).thenReturn(mortRes);
        when(client.getRentHouseLoanProducts(anyString(), anyInt())).thenReturn(rentRes);
        when(client.getCreditLoanProducts(anyString(), anyInt())).thenReturn(creditRes);

        // when
        int total = service.syncLoans(2);

        // then
        assertThat(total).isEqualTo(0);
        // bases 가 null 이라 financeProducts / loanOption 저장 로직은 안 타야 함
        verify(financeProductsRepository, never()).save(any());
        verify(loanOptionRepository, never()).save(any());
    }

    @Test
    void syncLoanProductType_throwsException_whenUnsupportedType() {
        // given & when & then
        assertThatThrownBy(() ->
                service.syncLoanProductType(FinanceProductType.DEPOSIT, 3)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported loan type");
    }
}
