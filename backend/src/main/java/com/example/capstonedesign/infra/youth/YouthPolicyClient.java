package com.example.capstonedesign.infra.youth;

import com.example.capstonedesign.domain.youthpolicies.dto.response.YouthPolicyApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * YouthPolicyClient
 * -------------------------------------------------
 * - 온통청년(Youth Center) 정책 API 클라이언트
 * - 정책 목록 조회 (키워드, 지역코드, 페이지 단위)
 */
@Component
@RequiredArgsConstructor
public class YouthPolicyClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${youth.api.key}")
    private String apiKey;

    private static final String BASE_URL = "https://www.youthcenter.go.kr/go/ythip/getPlcy";

    /**
     * 청년정책 목록 조회
     *
     * @param pageNum    페이지 번호
     * @param pageSize   페이지 크기
     * @param keyword    검색 키워드
     * @param regionCode 지역 코드 (zipCd)
     * @return YouthPolicyApiResponse (정책 목록 응답)
     */
    public YouthPolicyApiResponse fetchPolicies(int pageNum, int pageSize, String keyword, String regionCode) {
        String url = UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("apiKeyNm", apiKey)
                .queryParam("rtnType", "json")
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize)
                .queryParam("plcyKywdNm", keyword)
                .queryParam("zipCd", regionCode)
                .build()
                .toUriString();

        return restTemplate.getForObject(url, YouthPolicyApiResponse.class);
    }
}
