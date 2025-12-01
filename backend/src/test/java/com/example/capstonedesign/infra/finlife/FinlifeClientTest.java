package com.example.capstonedesign.infra.finlife;

import com.example.capstonedesign.common.exception.ApiException;
import com.example.capstonedesign.common.exception.ErrorCode;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinlifeCreditLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinlifeMortgageLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinlifeRentLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.infra.finlife.dto.FinlifeCompanySearchResponse;
import com.example.capstonedesign.infra.finlife.dto.FinlifeProductResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FinlifeClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FinlifeClient finlifeClient;

    @Captor
    private ArgumentCaptor<String> urlCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // @Value 필드 주입 대신 Reflection으로 세팅
        ReflectionTestUtils.setField(finlifeClient, "baseUrl", "http://finlife.test/finlifeapi");
        ReflectionTestUtils.setField(finlifeClient, "auth", "TEST-AUTH-KEY");
    }

    /* -----------------------------------------
     * fetchProducts (JsonNode)
     * ----------------------------------------- */

    @Test
    @DisplayName("fetchProducts(type, pageNo) - 그룹 없이 정상 호출")
    void fetchProducts_withoutGroup_success() {
        // given
        FinanceProductType type = FinanceProductType.DEPOSIT; // enum 그대로 사용
        int pageNo = 1;

        ObjectNode body = objectMapper.createObjectNode();
        body.put("result", "ok");

        ResponseEntity<JsonNode> response =
                new ResponseEntity<>(body, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenReturn(response);

        // when
        JsonNode result = finlifeClient.fetchProducts(type, pageNo);

        // then
        assertThat(result).isSameAs(body);

        verify(restTemplate).getForEntity(urlCaptor.capture(), eq(JsonNode.class));
        String url = urlCaptor.getValue();

        // URL에 endpoint, auth, pageNo 포함됐는지 확인
        assertThat(url).contains(type.getEndpoint() + ".json");
        assertThat(url).contains("auth=TEST-AUTH-KEY");
        assertThat(url).contains("pageNo=" + pageNo);
        // topFinGrpNo는 없어야 함
        assertThat(url).doesNotContain("topFinGrpNo=");
    }

    @Test
    @DisplayName("fetchProducts(type, topFinGrpNo, pageNo) - 그룹 포함 정상 호출")
    void fetchProducts_withGroup_success() {
        // given
        FinanceProductType type = FinanceProductType.SAVING;
        String topFinGrpNo = "020000"; // 예: 은행
        int pageNo = 2;

        ObjectNode body = objectMapper.createObjectNode();
        body.put("result", "ok");

        ResponseEntity<JsonNode> response =
                new ResponseEntity<>(body, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenReturn(response);

        // when
        JsonNode result = finlifeClient.fetchProducts(type, topFinGrpNo, pageNo);

        // then
        assertThat(result).isSameAs(body);

        verify(restTemplate).getForEntity(urlCaptor.capture(), eq(JsonNode.class));
        String url = urlCaptor.getValue();

        assertThat(url).contains(type.getEndpoint() + ".json");
        assertThat(url).contains("auth=TEST-AUTH-KEY");
        assertThat(url).contains("pageNo=" + pageNo);
        assertThat(url).contains("topFinGrpNo=" + topFinGrpNo);
    }

    @Test
    @DisplayName("fetchProducts - 2xx가 아니거나 body가 null이면 ApiException 발생")
    void fetchProducts_statusNot2xxOrBodyNull_throwsApiException() {
        // given
        FinanceProductType type = FinanceProductType.DEPOSIT;
        int pageNo = 1;

        // (1) 400 BAD_REQUEST 케이스
        ResponseEntity<JsonNode> badResponse =
                new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);

        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenReturn(badResponse);

        // when & then
        assertThatThrownBy(() -> finlifeClient.fetchProducts(type, pageNo))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_ERROR);

        // (2) 200 OK지만 body == null 인 케이스도 한 번 더
        ResponseEntity<JsonNode> nullBodyResponse =
                new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenReturn(nullBodyResponse);

        assertThatThrownBy(() -> finlifeClient.fetchProducts(type, pageNo))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_ERROR);
    }

    @Test
    @DisplayName("fetchProducts - RestClientException 발생 시 ApiException으로 래핑")
    void fetchProducts_restClientException_throwsApiException() {
        // given
        FinanceProductType type = FinanceProductType.DEPOSIT;
        int pageNo = 1;

        when(restTemplate.getForEntity(anyString(), eq(JsonNode.class)))
                .thenThrow(new RestClientException("Connection error"));

        // when & then
        assertThatThrownBy(() -> finlifeClient.fetchProducts(type, pageNo))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_ERROR);
    }

    /* -----------------------------------------
     * 예금 / 적금 / 대출 - 타입별 Typed 메서드
     * 내부적으로 getProductResponseTyped 사용
     * ----------------------------------------- */

    @Nested
    @DisplayName("예금/적금/대출 typed 메서드")
    class TypedProductMethods {

        @Test
        @DisplayName("getDepositProducts - 정상 호출")
        void getDepositProducts_success() {
            FinlifeProductResponse mockRes = mock(FinlifeProductResponse.class);

            when(restTemplate.getForObject(anyString(), eq(FinlifeProductResponse.class)))
                    .thenReturn(mockRes);

            FinlifeProductResponse result =
                    finlifeClient.getDepositProducts("020000", 1);

            assertThat(result).isSameAs(mockRes);

            verify(restTemplate).getForObject(urlCaptor.capture(), eq(FinlifeProductResponse.class));
            String url = urlCaptor.getValue();
            assertThat(url).contains(FinanceProductType.DEPOSIT.getEndpoint());
            assertThat(url).contains("auth=TEST-AUTH-KEY");
            assertThat(url).contains("pageNo=1");
            assertThat(url).contains("topFinGrpNo=020000");
        }

        @Test
        @DisplayName("getSavingProducts - 정상 호출")
        void getSavingProducts_success() {
            FinlifeProductResponse mockRes = mock(FinlifeProductResponse.class);

            when(restTemplate.getForObject(anyString(), eq(FinlifeProductResponse.class)))
                    .thenReturn(mockRes);

            FinlifeProductResponse result =
                    finlifeClient.getSavingProducts("030000", 3);

            assertThat(result).isSameAs(mockRes);

            verify(restTemplate).getForObject(urlCaptor.capture(), eq(FinlifeProductResponse.class));
            String url = urlCaptor.getValue();
            assertThat(url).contains(FinanceProductType.SAVING.getEndpoint());
            assertThat(url).contains("auth=TEST-AUTH-KEY");
            assertThat(url).contains("pageNo=3");
            assertThat(url).contains("topFinGrpNo=030000");
        }

        @Test
        @DisplayName("getMortgageLoanProducts - 정상 호출")
        void getMortgageLoanProducts_success() {
            FinlifeMortgageLoanResponse mockRes = mock(FinlifeMortgageLoanResponse.class);

            when(restTemplate.getForObject(anyString(), eq(FinlifeMortgageLoanResponse.class)))
                    .thenReturn(mockRes);

            FinlifeMortgageLoanResponse result =
                    finlifeClient.getMortgageLoanProducts(null, 1);

            assertThat(result).isSameAs(mockRes);

            verify(restTemplate).getForObject(urlCaptor.capture(), eq(FinlifeMortgageLoanResponse.class));
            String url = urlCaptor.getValue();
            assertThat(url).contains(FinanceProductType.MORTGAGE_LOAN.getEndpoint());
            assertThat(url).contains("pageNo=1");
            // 그룹 null이므로 topFinGrpNo 없어야 함
            assertThat(url).doesNotContain("topFinGrpNo=");
        }

        @Test
        @DisplayName("getRentHouseLoanProducts - 정상 호출")
        void getRentHouseLoanProducts_success() {
            FinlifeRentLoanResponse mockRes = mock(FinlifeRentLoanResponse.class);

            when(restTemplate.getForObject(anyString(), eq(FinlifeRentLoanResponse.class)))
                    .thenReturn(mockRes);

            FinlifeRentLoanResponse result =
                    finlifeClient.getRentHouseLoanProducts("050000", 4);

            assertThat(result).isSameAs(mockRes);

            verify(restTemplate).getForObject(urlCaptor.capture(), eq(FinlifeRentLoanResponse.class));
            String url = urlCaptor.getValue();
            assertThat(url).contains(FinanceProductType.RENT_HOUSE_LOAN.getEndpoint());
            assertThat(url).contains("pageNo=4");
            assertThat(url).contains("topFinGrpNo=050000");
        }

        @Test
        @DisplayName("getCreditLoanProducts - 정상 호출")
        void getCreditLoanProducts_success() {
            FinlifeCreditLoanResponse mockRes = mock(FinlifeCreditLoanResponse.class);

            when(restTemplate.getForObject(anyString(), eq(FinlifeCreditLoanResponse.class)))
                    .thenReturn(mockRes);

            FinlifeCreditLoanResponse result =
                    finlifeClient.getCreditLoanProducts("060000", 5);

            assertThat(result).isSameAs(mockRes);

            verify(restTemplate).getForObject(urlCaptor.capture(), eq(FinlifeCreditLoanResponse.class));
            String url = urlCaptor.getValue();
            assertThat(url).contains(FinanceProductType.CREDIT_LOAN.getEndpoint());
            assertThat(url).contains("pageNo=5");
            assertThat(url).contains("topFinGrpNo=060000");
        }

        @Test
        @DisplayName("Typed 메서드 - RestClientException 시 ApiException 발생")
        void typedMethods_restClientException_throwsApiException() {
            when(restTemplate.getForObject(anyString(), any()))
                    .thenThrow(new RestClientException("timeout"));

            assertThatThrownBy(() ->
                    finlifeClient.getDepositProducts("020000", 1)
            )
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /* -----------------------------------------
     * getCompanies
     * ----------------------------------------- */

    @Test
    @DisplayName("getCompanies - topFinGrpNo 포함, 정상 호출")
    void getCompanies_withGroup_success() {
        FinlifeCompanySearchResponse mockRes = mock(FinlifeCompanySearchResponse.class);

        when(restTemplate.getForObject(anyString(), eq(FinlifeCompanySearchResponse.class)))
                .thenReturn(mockRes);

        FinlifeCompanySearchResponse result =
                finlifeClient.getCompanies("020000", 2);

        assertThat(result).isSameAs(mockRes);

        verify(restTemplate).getForObject(urlCaptor.capture(), eq(FinlifeCompanySearchResponse.class));
        String url = urlCaptor.getValue();

        assertThat(url).contains("/companySearch.json");
        assertThat(url).contains("auth=TEST-AUTH-KEY");
        assertThat(url).contains("pageNo=2");
        assertThat(url).contains("topFinGrpNo=020000");
    }

    @Test
    @DisplayName("getCompanies - topFinGrpNo null이면 파라미터 없이 호출")
    void getCompanies_withoutGroup_success() {
        FinlifeCompanySearchResponse mockRes = mock(FinlifeCompanySearchResponse.class);

        when(restTemplate.getForObject(anyString(), eq(FinlifeCompanySearchResponse.class)))
                .thenReturn(mockRes);

        FinlifeCompanySearchResponse result =
                finlifeClient.getCompanies(null, 1);

        assertThat(result).isSameAs(mockRes);

        verify(restTemplate).getForObject(urlCaptor.capture(), eq(FinlifeCompanySearchResponse.class));
        String url = urlCaptor.getValue();

        assertThat(url).contains("/companySearch.json");
        assertThat(url).contains("pageNo=1");
        assertThat(url).doesNotContain("topFinGrpNo=");
    }

    @Test
    @DisplayName("getCompanies - RestClientException 발생 시 ApiException 발생")
    void getCompanies_restClientException_throwsApiException() {
        when(restTemplate.getForObject(anyString(), eq(FinlifeCompanySearchResponse.class)))
                .thenThrow(new RestClientException("error"));

        assertThatThrownBy(() ->
                finlifeClient.getCompanies("020000", 1)
        )
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_ERROR);
    }
}
