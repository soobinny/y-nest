package com.example.capstonedesign.infra.youth;

import com.example.capstonedesign.domain.youthpolicies.dto.response.YouthPolicyApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * YouthPolicyClient
 * -------------------------------------------------
 * 온통청년(Youth Center) 정책 API를 호출하여 정책 데이터를 조회하는 클라이언트
 * - 키워드, 지역 코드, 페이지 정보를 기반으로 정책 목록을 요청
 */
@Slf4j
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

        try {
            // 1. JSON 요청 헤더 명시
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 2. exchange로 요청 (getForObject보다 안전)
            ResponseEntity<YouthPolicyApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    YouthPolicyApiResponse.class
            );

            // 3. 정상 응답 반환
            return response.getBody();

        } catch (HttpServerErrorException e) {
            // 4. 서버 내부 오류 (HTML 반환 포함)
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("<html")) {
                log.error("❌ 온통청년 서버에서 HTML 에러 페이지 반환됨 (점검 중 가능성 높음)");
            } else {
                log.error("❌ 온통청년 서버 오류 발생: {}", e.getMessage());
            }
            // 앱 종료 방지 → 빈 응답으로 대체
            return YouthPolicyApiResponse.empty();

        } catch (Exception e) {
            // 5. 기타 예외 (연결 실패, 파싱 실패 등)
            log.error("⚠️ 청년정책 API 호출 중 예외 발생: {}", e.getMessage());
            return YouthPolicyApiResponse.empty();
        }
    }
}