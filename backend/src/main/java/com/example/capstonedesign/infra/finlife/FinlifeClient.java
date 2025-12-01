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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 금융감독원 Open API와 통신하는 클라이언트 서비스
 * 예금/적금/대출/금융 회사 목록 등의 데이터를 외부 API로부터 조회
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinlifeClient {

    private final RestTemplate restTemplate;

    @Value("${finlife.base-url}")
    private String baseUrl;  // 예: http://finlife.fss.or.kr/finlifeapi

    @Value("${finlife.api-key}")
    private String auth;     // API 인증키

    /* ---------------- 기본 JSON 조회 메서드 ---------------- */

    public JsonNode fetchProducts(FinanceProductType type, int pageNo) {
        return fetchProducts(type, null, pageNo);
    }

    public JsonNode fetchProducts(FinanceProductType type, String topFinGrpNo, int pageNo) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(baseUrl + "/" + type.getEndpoint() + ".json")
                    .queryParam("auth", auth)
                    .queryParam("pageNo", pageNo);

            if (topFinGrpNo != null && !topFinGrpNo.isBlank()) {
                builder.queryParam("topFinGrpNo", topFinGrpNo);
            }

            String url = builder.toUriString();
            log.debug("[FinlifeClient] GET {}", url);

            ResponseEntity<JsonNode> res = restTemplate.getForEntity(url, JsonNode.class);
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                throw new ApiException(ErrorCode.EXTERNAL_API_ERROR);
            }
            return res.getBody();
        } catch (RestClientException e) {
            log.error("Finlife API call failed: type={}, group={}, pageNo={}, msg={}",
                    type.getDisplayName(), topFinGrpNo, pageNo, e.getMessage());
            throw new ApiException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /* ---------------- 예금 / 적금 ---------------- */

    public FinlifeProductResponse getDepositProducts(String topFinGrpNo, int pageNo) {
        return getProductResponseTyped(FinanceProductType.DEPOSIT, topFinGrpNo, pageNo, FinlifeProductResponse.class);
    }

    public FinlifeProductResponse getSavingProducts(String topFinGrpNo, int pageNo) {
        return getProductResponseTyped(FinanceProductType.SAVING, topFinGrpNo, pageNo, FinlifeProductResponse.class);
    }

    /* ---------------- 대출 (3종 분리) ---------------- */

    // 주택담보대출
    public FinlifeMortgageLoanResponse getMortgageLoanProducts(String topFinGrpNo, int pageNo) {
        return getProductResponseTyped(FinanceProductType.MORTGAGE_LOAN, topFinGrpNo, pageNo, FinlifeMortgageLoanResponse.class);
    }

    // 전세자금대출
    public FinlifeRentLoanResponse getRentHouseLoanProducts(String topFinGrpNo, int pageNo) {
        return getProductResponseTyped(FinanceProductType.RENT_HOUSE_LOAN, topFinGrpNo, pageNo, FinlifeRentLoanResponse.class);
    }

    // 개인신용대출
    public FinlifeCreditLoanResponse getCreditLoanProducts(String topFinGrpNo, int pageNo) {
        return getProductResponseTyped(FinanceProductType.CREDIT_LOAN, topFinGrpNo, pageNo, FinlifeCreditLoanResponse.class);
    }

    /* ---------------- 금융 회사 목록 조회 ---------------- */

    public FinlifeCompanySearchResponse getCompanies(String topFinGrpNo, int pageNo) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(baseUrl + "/companySearch.json")
                    .queryParam("auth", auth)
                    .queryParam("pageNo", pageNo);

            if (topFinGrpNo != null && !topFinGrpNo.isBlank()) {
                builder.queryParam("topFinGrpNo", topFinGrpNo);
            }

            String url = builder.toUriString();
            log.debug("[FinlifeClient] GET {}", url);

            return restTemplate.getForObject(url, FinlifeCompanySearchResponse.class);
        } catch (RestClientException e) {
            log.error("Finlife companies call failed: group={}, pageNo={}, msg={}", topFinGrpNo, pageNo, e.getMessage());
            throw new ApiException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /* ---------------- 공통 Typed 호출 메서드 ---------------- */

    private <T> T getProductResponseTyped(FinanceProductType type, String topFinGrpNo, int pageNo, Class<T> clazz) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(baseUrl + "/" + type.getEndpoint() + ".json")
                    .queryParam("auth", auth)
                    .queryParam("pageNo", pageNo);

            if (topFinGrpNo != null && !topFinGrpNo.isBlank()) {
                builder.queryParam("topFinGrpNo", topFinGrpNo);
            }

            String url = builder.toUriString();
            log.debug("[FinlifeClient] GET {}", url);

            return restTemplate.getForObject(url, clazz);
        } catch (RestClientException e) {
            log.error("Finlife typed call failed: type={}, group={}, pageNo={}, msg={}",
                    type.getDisplayName(), topFinGrpNo, pageNo, e.getMessage());
            throw new ApiException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }
}
