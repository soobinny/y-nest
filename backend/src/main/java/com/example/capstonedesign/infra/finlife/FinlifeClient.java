package com.example.capstonedesign.infra.finlife;

import com.example.capstonedesign.infra.finlife.dto.FinlifeCompanySearchResponse;
import com.example.capstonedesign.infra.finlife.dto.FinlifeProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class FinlifeClient {

    private final RestClient restClient = RestClient.create();

    @Value("${finlife.base-url}") private String baseUrl;
    @Value("${finlife.api-key}") private String apiKey;

    // 회사 목록 (권역별)
    public FinlifeCompanySearchResponse getCompanies(String topFinGrpNo, int pageNo) {
        String url = String.format("%s/companySearch.json?auth=%s&topFinGrpNo=%s&pageNo=%d",
                baseUrl, apiKey, topFinGrpNo, pageNo);
        return restClient.get().uri(url).retrieve().body(FinlifeCompanySearchResponse.class);
    }

    // 예금
    public FinlifeProductResponse getDepositProducts(String topFinGrpNo, int pageNo) {
        String url = String.format("%s/depositProductsSearch.json?auth=%s&topFinGrpNo=%s&pageNo=%d",
                baseUrl, apiKey, topFinGrpNo, pageNo);
        return restClient.get().uri(url).retrieve().body(FinlifeProductResponse.class);
    }

    // 적금
    public FinlifeProductResponse getSavingProducts(String topFinGrpNo, int pageNo) {
        String url = String.format("%s/savingProductsSearch.json?auth=%s&topFinGrpNo=%s&pageNo=%d",
                baseUrl, apiKey, topFinGrpNo, pageNo);
        return restClient.get().uri(url).retrieve().body(FinlifeProductResponse.class);
    }
}
