//package com.example.capstonedesign.domain.lhapi.service;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatusCode;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//
//@Service
//@RequiredArgsConstructor
//public class LhApiService {
//
//    private final WebClient.Builder webClientBuilder;
//
//    @Value("${external.lh.base-url}")
//    private String baseUrl;
//
//    @Value("${external.lh.endpoint}")
//    private String endpoint;
//
//    @Value("${external.lh.key}")
//    private String apiKey;
//
//    /**
//     * LH 분양/임대 공고 조회
//     * - 필수 파라미터: serviceKey, PG_SZ, PAGE
//     * - base-url은 .../lhLeaseNoticeInfo1 이고, pathSegment로 endpoint(lhLeaseNoticeInfo1)를 붙인다.
//     */
//    public String getLeaseNotices(int page, int size) {
//        try {
//            return webClientBuilder
//                    .baseUrl(baseUrl)
//                    .build()
//                    .get()
//                    .uri(uriBuilder -> uriBuilder
//                            .pathSegment(endpoint)                // => 최종 URL: .../lhLeaseNoticeInfo1/lhLeaseNoticeInfo1
//                            .queryParam("serviceKey", apiKey)     // 원본 키를 넣으면 WebClient가 인코딩 처리
//                            .queryParam("PG_SZ", size)
//                            .queryParam("PAGE", page)
//                            .build())
//                    .retrieve()
//                    .onStatus(HttpStatusCode::isError, resp ->
//                            resp.createException().flatMap(e -> {
//                                // 여기서 바로 던져서 GlobalExceptionHandler 타게 함
//                                return reactor.core.publisher.Mono.error(e);
//                            })
//                    )
//                    .bodyToMono(String.class)
//                    .block();
//        } catch (WebClientResponseException e) {
//            // 상태/본문 그대로 GlobalExceptionHandler로
//            throw e;
//        } catch (Exception e) {
//            // 기타 예외도 전역 핸들러로
//            throw new RuntimeException("LH API 호출 중 오류가 발생했습니다.", e);
//        }
//    }
//}
