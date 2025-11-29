package com.example.capstonedesign.application.ingest.SH;

import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import com.example.capstonedesign.domain.shannouncements.entity.RecruitStatus;
import com.example.capstonedesign.domain.shannouncements.entity.SHHousingCategory;
import com.example.capstonedesign.domain.shannouncements.entity.ShAnnouncement;
import com.example.capstonedesign.domain.shannouncements.repository.ShAnnouncementRepository;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ShIngestService 단위 테스트
 * - crawlAll 정상 플로우
 * - crawlAll 예외 플로우(catch)
 * - syncNotices → crawlAll 위임
 * - upsert 신규/기존 두 분기
 */
@ExtendWith(MockitoExtension.class)
class ShIngestServiceTest {

    @Mock
    ShAnnouncementRepository repo;

    @Mock
    ProductsRepository productsRepository;

    @InjectMocks
    ShIngestService shIngestService;

    // -------------------------------------------------------------------------
    // 1. crawlAll() 기본 성공 플로우
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("crawlAll()이 Jsoup를 통해 목록/상세를 파싱하고 저장 로직까지 흘러간다")
    void crawlAll_crawlsAndUpsertsNotices() throws Exception {
        // given: 테스트용 HTML (목록 + 상세를 한 문서로 통합해도 됨)
        String html = "<html><body>"
                + "<table id=\"listTb\"><tbody>"
                + "<tr>"
                + "<td class=\"num\">2024-11-01</td>"   // 공고일
                + "<td class=\"num\">123</td>"          // 조회수
                + "<td>주택공급부</td>"                 // 부서
                + "<td class=\"txtL\">"
                + "  <a onclick=\"getDetailView('99999')\">강남 청년안심주택 모집공고</a>"
                + "</td>"
                + "</tr>"
                + "</tbody></table>"
                + "<div id=\"contents\"><p>본문 내용</p>"
                + "  <div class=\"attach\"><a href=\"/files/notice.pdf\">공고문</a></div>"
                + "</div>"
                + "</body></html>";

        // Jsoup.parse는 실제 메서드 사용 (static mock 적용 전이므로 영향 X)
        Document doc = Jsoup.parse(html);

        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);

            // 모든 Jsoup.connect(...) 호출을 같은 mock Connection으로 처리
            jsoup.when(() -> Jsoup.connect(anyString()))
                    .thenReturn(conn);

            when(conn.timeout(anyInt())).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(conn);
            when(conn.data(anyString(), anyString())).thenReturn(conn);
            when(conn.method(any(Connection.Method.class))).thenReturn(conn);
            // 목록/상세 둘 다 같은 Document를 반환해도 로직상 문제 없음
            when(conn.get()).thenReturn(doc);

            // Product 저장 시 그대로 넘겨받은 엔티티를 반환
            when(productsRepository.save(any(Products.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // upsert 분기에서 Optional.empty() → 신규 저장 경로로 가도록
            when(repo.findBySourceAndExternalId(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            // when
            shIngestService.crawlAll();

            // then
            verify(productsRepository, atLeastOnce()).save(any(Products.class));
            verify(repo, atLeastOnce()).save(any(ShAnnouncement.class));
        }
    }

    // -------------------------------------------------------------------------
    // 2. crawlAll() 예외 플로우 → catch (Exception e) 실행
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("crawlAll() 중 Jsoup 예외 발생 시 catch 블록에서 처리되고 밖으로 던지지 않는다")
    void crawlAll_handlesExceptionInCatchBlock() {
        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            // Jsoup.connect()가 항상 예외를 던지도록 설정 → catch 라인 강제 실행
            jsoup.when(() -> Jsoup.connect(anyString()))
                    .thenThrow(new RuntimeException("연결 실패"));

            // when & then: 예외가 밖으로 터지지 않아야 한다.
            assertDoesNotThrow(() -> shIngestService.crawlAll());
        }
    }

    // -------------------------------------------------------------------------
    // 3. syncNotices() → crawlAll() 위임
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("syncNotices()는 crawlAll()을 단순 래핑한다")
    void syncNotices_delegatesToCrawlAll() {
        // @InjectMocks가 아니라, spy로 새 인스턴스를 만들어서 내부 호출만 검증
        ShIngestService spyService = Mockito.spy(new ShIngestService(repo, productsRepository));

        doNothing().when(spyService).crawlAll();

        // when
        spyService.syncNotices();

        // then
        verify(spyService, times(1)).crawlAll();
    }

    // -------------------------------------------------------------------------
    // 4. upsert() - 기존 데이터 없음 → 신규 저장 분기
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("upsert() - 기존 데이터가 없으면 신규로 저장한다")
    void upsert_insertsWhenNotExists() throws Exception {
        Products product = Products.builder()
                .type(ProductType.HOUSING)
                .name("테스트 상품")
                .provider("SH 서울주택도시공사")
                .detailUrl("https://test-detail")
                .build();

        ShAnnouncement target = ShAnnouncement.builder()
                .product(product)
                .source("i-sh")
                .externalId("123")
                .title("신규 공고")
                .department("주택공급부")
                .postDate(LocalDate.of(2024, 11, 1))
                .views(100)
                .recruitStatus(RecruitStatus.now)
                .supplyType("청년안심주택")
                .category(SHHousingCategory.주택임대)
                .region("강남")
                .contentHtml("<p>내용</p>")
                .attachments("[]")
                .detailUrl("https://test-detail")
                .crawledAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(repo.findBySourceAndExternalId("i-sh", "123"))
                .thenReturn(Optional.empty());

        // private void upsert(ShAnnouncement a) 호출 (리플렉션)
        Method m = ShIngestService.class.getDeclaredMethod("upsert", ShAnnouncement.class);
        m.setAccessible(true);

        // when
        m.invoke(shIngestService, target);

        // then
        verify(repo, times(1)).save(target);
    }

    // -------------------------------------------------------------------------
    // 5. upsert() - 기존 데이터 있음 → 필드 업데이트 분기
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("upsert() - 기존 데이터가 있으면 필드를 갱신하고 저장한다")
    void upsert_updatesWhenExists() throws Exception {
        Products product = Products.builder()
                .type(ProductType.HOUSING)
                .name("기존 상품")
                .provider("SH 서울주택도시공사")
                .detailUrl("https://old-detail")
                .build();

        ShAnnouncement existing = ShAnnouncement.builder()
                .product(product)
                .source("i-sh")
                .externalId("999")
                .title("기존 공고")
                .department("기존 부서")
                .postDate(LocalDate.of(2020, 1, 1))
                .views(1)
                .recruitStatus(RecruitStatus.now)
                .supplyType("청년안심주택")
                .category(SHHousingCategory.주택임대)
                .region("서울")
                .contentHtml("<p>old</p>")
                .attachments("[]")
                .detailUrl("https://old-detail")
                .crawledAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        ShAnnouncement updated = ShAnnouncement.builder()
                .product(product) // 실제로는 새 product를 넣더라도, 코드에서 기존 product로 덮어씀
                .source("i-sh")
                .externalId("999")
                .title("업데이트된 공고")
                .department("신규 부서")
                .postDate(LocalDate.of(2024, 11, 1))
                .views(10)
                .recruitStatus(RecruitStatus.now)
                .supplyType("청년안심주택")
                .category(SHHousingCategory.주택임대)
                .region("강남")
                .contentHtml("<p>new</p>")
                .attachments("[{\"name\":\"공고문\",\"url\":\"/files/notice.pdf\"}]")
                .detailUrl("https://new-detail")
                .crawledAt(existing.getCrawledAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(repo.findBySourceAndExternalId("i-sh", "999"))
                .thenReturn(Optional.of(existing));

        Method m = ShIngestService.class.getDeclaredMethod("upsert", ShAnnouncement.class);
        m.setAccessible(true);

        // when
        m.invoke(shIngestService, updated);

        // then
        ArgumentCaptor<ShAnnouncement> captor = ArgumentCaptor.forClass(ShAnnouncement.class);
        verify(repo, times(1)).save(captor.capture());

        ShAnnouncement saved = captor.getValue();
        // product는 기존 것을 유지해야 함
        assertEquals(product, saved.getProduct());
        assertEquals("업데이트된 공고", saved.getTitle());
        assertEquals("신규 부서", saved.getDepartment());
        assertEquals(LocalDate.of(2024, 11, 1), saved.getPostDate());
        assertEquals(10, saved.getViews());
        assertEquals("강남", saved.getRegion());
        assertEquals("https://new-detail", saved.getDetailUrl());
    }
}
