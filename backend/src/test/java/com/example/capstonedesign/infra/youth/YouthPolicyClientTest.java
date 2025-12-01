package com.example.capstonedesign.infra.youth;

import com.example.capstonedesign.domain.youthpolicies.dto.response.YouthPolicyApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * YouthPolicyClient 단위 테스트
 * - RestTemplate을 Mock으로 교체해서 HTTP 호출 없이 로직만 검증
 */
class YouthPolicyClientTest {

    @Mock
    private RestTemplate restTemplate;

    private YouthPolicyClient youthPolicyClient;

    @Captor
    private ArgumentCaptor<String> urlCaptor;

    @Captor
    private ArgumentCaptor<HttpEntity<String>> httpEntityCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // YouthPolicyClient 인스턴스 생성 (기본 생성자 사용)
        youthPolicyClient = new YouthPolicyClient();

        // private final RestTemplate restTemplate 필드를 Mock으로 교체
        ReflectionTestUtils.setField(youthPolicyClient, "restTemplate", restTemplate);

        // @Value 필드 주입 대신 Reflection으로 API 키 세팅
        ReflectionTestUtils.setField(youthPolicyClient, "apiKey", "TEST-YOUTH-API-KEY");
    }

    @Test
    @DisplayName("fetchPolicies - 정상 응답(JSON) 반환")
    void fetchPolicies_success() {
        // given
        int pageNum = 1;
        int pageSize = 10;
        String keyword = "청년";
        String regionCode = "11";

        YouthPolicyApiResponse mockResponse = mock(YouthPolicyApiResponse.class);

        ResponseEntity<YouthPolicyApiResponse> responseEntity =
                new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(YouthPolicyApiResponse.class)
        )).thenReturn(responseEntity);

        // when
        YouthPolicyApiResponse result =
                youthPolicyClient.fetchPolicies(pageNum, pageSize, keyword, regionCode);

        // then
        assertThat(result).isSameAs(mockResponse);

        // URL / 헤더 검증
        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.GET),
                httpEntityCaptor.capture(),
                eq(YouthPolicyApiResponse.class)
        );

        String url = urlCaptor.getValue();
        HttpEntity<String> entity = httpEntityCaptor.getValue();
        HttpHeaders headers = entity.getHeaders();

        // 쿼리 파라미터 확인
        assertThat(url).contains("apiKeyNm=TEST-YOUTH-API-KEY");
        assertThat(url).contains("rtnType=json");
        assertThat(url).contains("pageNum=" + pageNum);
        assertThat(url).contains("pageSize=" + pageSize);
        assertThat(url).contains("plcyKywdNm=" + keyword);
        assertThat(url).contains("zipCd=" + regionCode);

        // 헤더 확인 (JSON 요청)
        assertThat(headers.getAccept()).containsExactly(MediaType.APPLICATION_JSON);
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("fetchPolicies - HttpServerErrorException + HTML 에러 페이지 → empty 응답 반환")
    void fetchPolicies_httpServerError_html() {
        // given
        int pageNum = 1;
        int pageSize = 10;

        HttpHeaders errorHeaders = new HttpHeaders();
        byte[] bodyBytes = "<html><body>Server error</body></html>".getBytes(StandardCharsets.UTF_8);

        HttpServerErrorException exception = new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                errorHeaders,
                bodyBytes,
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(YouthPolicyApiResponse.class)
        )).thenThrow(exception);

        // when
        YouthPolicyApiResponse result =
                youthPolicyClient.fetchPolicies(pageNum, pageSize, "청년", "11");

        // then
        assertThat(result).isNotNull();   // YouthPolicyApiResponse.empty() 기대
    }

    @Test
    @DisplayName("fetchPolicies - HttpServerErrorException + 일반 메시지 → empty 응답 반환")
    void fetchPolicies_httpServerError_noHtml() {
        // given
        int pageNum = 2;
        int pageSize = 20;

        HttpHeaders errorHeaders = new HttpHeaders();
        byte[] bodyBytes = "서버 오류 발생".getBytes(StandardCharsets.UTF_8);

        HttpServerErrorException exception = new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                errorHeaders,
                bodyBytes,
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(YouthPolicyApiResponse.class)
        )).thenThrow(exception);

        // when
        YouthPolicyApiResponse result =
                youthPolicyClient.fetchPolicies(pageNum, pageSize, null, null);

        // then
        assertThat(result).isNotNull();   // empty 응답
    }

    @Test
    @DisplayName("fetchPolicies - 기타 예외(RuntimeException 등) → empty 응답 반환")
    void fetchPolicies_genericException() {
        // given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(YouthPolicyApiResponse.class)
        )).thenThrow(new RuntimeException("Connection timeout"));

        // when
        YouthPolicyApiResponse result =
                youthPolicyClient.fetchPolicies(1, 10, "테스트", "11");

        // then
        assertThat(result).isNotNull();   // empty 응답
    }
}
