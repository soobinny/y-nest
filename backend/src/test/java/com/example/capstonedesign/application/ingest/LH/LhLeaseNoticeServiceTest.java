package com.example.capstonedesign.application.ingest.LH;

import com.example.capstonedesign.domain.housingannouncements.entity.LhNotice;
import com.example.capstonedesign.domain.housingannouncements.repository.LhNoticeRepository;
import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * LhLeaseNoticeService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class LhLeaseNoticeServiceTest {

    @Mock
    LhNoticeRepository lhNoticeRepository;

    @Mock
    ProductsRepository productsRepository;

    @InjectMocks
    LhLeaseNoticeService service;

    // ---------------------------------------------------------------------
    // 1. 정상 플로우: JSON 응답 파싱 + 신규 공고 저장
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("fetchNotices() - LH API JSON 응답을 파싱해 신규 공고와 상품을 저장한다")
    void fetchNotices_parsesJsonAndSavesNewNotices() throws Exception {
        // given
        // 루트에 "response" 오브젝트가 있고 그 안에 dsList 배열이 있는 형태
        String json = """
            {
              "response": {
                "dsList": [
                  {
                    "PAN_NM": "LH 임대공고 1",
                    "PAN_NT_ST_DT": "2024-11-01",
                    "DTL_URL": "https://lh.or.kr/detail/1",
                    "UPP_AIS_TP_NM": "임대주택",
                    "AIS_TP_CD_NM": "일반공급",
                    "CNP_CD_NM": "서울특별시",
                    "PAN_SS": "공고중",
                    "CLSG_DT": "2024-11-30"
                  }
                ]
              }
            }
            """;

        ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        HttpURLConnection conn = mock(HttpURLConnection.class);
        when(conn.getResponseCode()).thenReturn(200);
        when(conn.getInputStream()).thenReturn(is);

        // new URL(apiUrl) 호출을 가로채서, 우리가 만든 mock URL을 주입
        try (MockedConstruction<URL> mockedUrl = Mockito.mockConstruction(
                URL.class,
                (mock, context) -> when(mock.openConnection()).thenReturn(conn)
        )) {
            // 중복 공고 없음 → 항상 Optional.empty()
            when(lhNoticeRepository.findByPanNmAndPanNtStDt(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            // save(...) 는 그대로 받은 엔티티를 반환
            when(productsRepository.save(any(Products.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(lhNoticeRepository.save(any(LhNotice.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            service.fetchNotices();

            // then
            ArgumentCaptor<Products> productCaptor = ArgumentCaptor.forClass(Products.class);
            ArgumentCaptor<LhNotice> noticeCaptor = ArgumentCaptor.forClass(LhNotice.class);

            verify(productsRepository, times(1)).save(productCaptor.capture());
            verify(lhNoticeRepository, times(1)).save(noticeCaptor.capture());

            Products savedProduct = productCaptor.getValue();
            LhNotice savedNotice = noticeCaptor.getValue();

            // Products 검증
            assertThat(savedProduct.getType()).isEqualTo(ProductType.HOUSING);
            assertThat(savedProduct.getName()).isEqualTo("LH 임대공고 1");
            assertThat(savedProduct.getProvider()).isEqualTo("LH 한국토지주택공사");
            assertThat(savedProduct.getDetailUrl()).isEqualTo("https://lh.or.kr/detail/1");

            // LhNotice 검증
            assertThat(savedNotice.getProduct()).isEqualTo(savedProduct);
            assertThat(savedNotice.getPanNm()).isEqualTo("LH 임대공고 1");
            assertThat(savedNotice.getPanNtStDt()).isEqualTo("2024-11-01");
            assertThat(savedNotice.getDtlUrl()).isEqualTo("https://lh.or.kr/detail/1");
        }
    }

    // ---------------------------------------------------------------------
    // 2. 예외 플로우: 내부에서 예외 발생 시 catch 블록이 처리하는지
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("fetchNotices() - 내부에서 예외가 발생해도 메서드가 예외를 던지지 않는다")
    void fetchNotices_handlesExceptionInCatchBlock() {
        // new URL(apiUrl) → openConnection() 에서 예외 발생시키기
        try (MockedConstruction<URL> mockedUrl = Mockito.mockConstruction(
                URL.class,
                (mock, context) -> when(mock.openConnection())
                        .thenThrow(new RuntimeException("연결 실패"))
        )) {
            assertDoesNotThrow(() -> service.fetchNotices());
        }

        // 예외 때문에 아무 것도 저장되지 않아야 함
        verify(lhNoticeRepository, never()).save(any());
        verify(productsRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // 2-1. HTTP 응답 코드가 200이 아닌 경우 → 에러 로그 후 즉시 종료
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("fetchNotices() - HTTP 응답 코드가 200이 아니면 에러 로그 후 루프를 종료한다")
    void fetchNotices_stopsWhenResponseCodeIsNot200() throws Exception {
        // given
        HttpURLConnection conn = mock(HttpURLConnection.class);
        // 200이 아닌 코드로 세팅
        when(conn.getResponseCode()).thenReturn(500);

        try (MockedConstruction<URL> mockedUrl = Mockito.mockConstruction(
                URL.class,
                (mock, context) -> when(mock.openConnection()).thenReturn(conn)
        )) {
            // when & then
            // 예외가 밖으로 던져지면 안 된다
            assertDoesNotThrow(() -> service.fetchNotices());

            // 200이 아니므로, JSON 파싱/저장은 한 번도 일어나지 않아야 함
            verify(lhNoticeRepository, never()).save(any());
            verify(productsRepository, never()).save(any());

            // 연결 해제는 호출되었는지 확인 (원하면 atLeastOnce()로 바꿔도 됨)
            verify(conn, times(1)).disconnect();
        }
    }

    // ---------------------------------------------------------------------
    // 3. syncNotices() → fetchNotices() 래핑
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("syncNotices()는 fetchNotices()를 단순 래핑한다")
    void syncNotices_delegatesToFetchNotices() {
        // @InjectMocks 대신, 명시적으로 spy 생성 (fetchNotices만 감시)
        LhLeaseNoticeService spyService =
                Mockito.spy(new LhLeaseNoticeService(lhNoticeRepository, productsRepository));

        doNothing().when(spyService).fetchNotices();

        // when
        spyService.syncNotices();

        // then
        verify(spyService, times(1)).fetchNotices();
    }
}
