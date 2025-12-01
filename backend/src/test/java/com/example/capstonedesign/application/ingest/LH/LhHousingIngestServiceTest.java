package com.example.capstonedesign.application.ingest.LH;

import com.example.capstonedesign.domain.housingannouncements.entity.HousingAnnouncements;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingStatus;
import com.example.capstonedesign.domain.housingannouncements.repository.HousingAnnouncementsRepository;
import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LhHousingIngestService 단위 테스트
 * - Jsoup를 static mock으로 대체하여 실제 HTTP 호출 없이 크롤링/파싱 로직 검증
 * - ingest() → crawlAllPagesWithState → crawlAndSave까지 주요 흐름 커버
 * - 주요 private 유틸 메서드도 reflection으로 개별 검증
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LhHousingIngestServiceTest {

    @Mock
    ProductsRepository productsRepository;

    @Mock
    HousingAnnouncementsRepository housingRepository;

    @InjectMocks
    LhHousingIngestService service;

    // ---------------------------------------------------------------------
    // 1. ingest() 정상 플로우:
    //    - RENT_URL / SALE_URL 각각에 대해 1페이지만 성공적으로 파싱 & 저장
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("ingest() - 임대/분양 각 1페이지를 파싱해 Products/HousingAnnouncements를 저장한다")
    void ingest_parsesFirstPageAndSaves() throws Exception {
        // LH 목록 HTML (임대/분양 공통으로 사용)
        String html = """
            <html><body>
              <form name="pagingForm">
                <input type="hidden" name="listCo" value="50"/>
              </form>
              <table>
                <tbody>
                  <tr>
                    <td>0</td>
                    <td>1</td>
                    <td>
                      <a href="https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancInfo.do?panId=123&aisTpCd=AA&uppAisTpCd=BB&ccrCnntSysDsCd=CC">
                        LH 공고 타이틀
                      </a>
                    </td>
                    <td>서울특별시 강남구</td>
                    <td>-</td>
                    <td>2024.11.01</td>
                    <td>2024.11.30</td>
                    <td>공고중</td>
                  </tr>
                </tbody>
              </table>
            </body></html>
            """;

        // Jsoup.parse는 실제로 사용 (문자열 → Document)
        Document doc = Jsoup.parse(html, "https://apply.lh.or.kr");

        Connection connMock = mock(Connection.class);
        Connection.Response resMock = mock(Connection.Response.class);

        when(connMock.userAgent(anyString())).thenReturn(connMock);
        when(connMock.referrer(anyString())).thenReturn(connMock);
        when(connMock.timeout(anyInt())).thenReturn(connMock);
        when(connMock.method(any(Connection.Method.class))).thenReturn(connMock);
        when(connMock.followRedirects(anyBoolean())).thenReturn(connMock);
        when(connMock.header(anyString(), anyString())).thenReturn(connMock);
        when(connMock.data(anyString(), anyString())).thenReturn(connMock);

        when(connMock.execute()).thenReturn(resMock);
        when(resMock.cookies()).thenReturn(new HashMap<>());
        when(resMock.parse()).thenReturn(doc);

        // Jsoup.connect(...)를 모두 같은 mock Connection으로 처리
        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString()))
                    .thenReturn(connMock);

            // Products / HousingAnnouncements 저장 시 그대로 반환
            when(productsRepository.findByDetailUrl(anyString()))
                    .thenReturn(Optional.empty());

            when(productsRepository.save(any(Products.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(housingRepository.findByProduct(any(Products.class)))
                    .thenReturn(Optional.empty());

            when(housingRepository.save(any(HousingAnnouncements.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            service.ingest();

            // then
            // 임대 + 분양 두 번 호출되므로, 최소 2회 이상 저장이 발생해야 함
            ArgumentCaptor<Products> productCaptor = ArgumentCaptor.forClass(Products.class);
            ArgumentCaptor<HousingAnnouncements> haCaptor = ArgumentCaptor.forClass(HousingAnnouncements.class);

            verify(productsRepository, atLeast(2)).save(productCaptor.capture());
            verify(housingRepository, atLeast(2)).save(haCaptor.capture());

            // 하나만 잡아서 검증
            Products savedProduct = productCaptor.getAllValues().get(0);
            HousingAnnouncements savedHa = haCaptor.getAllValues().get(0);

            assertThat(savedProduct.getType()).isEqualTo(ProductType.HOUSING);
            assertThat(savedProduct.getName().trim()).isEqualTo("LH 공고 타이틀");
            assertThat(savedProduct.getProvider()).isEqualTo("LH");
            assertThat(savedProduct.getDetailUrl()).contains("panId=123");

            assertThat(savedHa.getProduct()).isEqualTo(savedProduct);
            assertThat(savedHa.getRegionName()).contains("강남구");
            assertThat(savedHa.getStatus()).isEqualTo(HousingStatus.공고중);
            assertThat(savedHa.getCategory()).isIn(HousingCategory.임대주택, HousingCategory.분양주택);
        }
    }

    // ---------------------------------------------------------------------
    // 2. ingest() 내부 - crawlAllPagesWithState 전체 예외 발생 (Jsoup.connect 자체 실패)
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("ingest() - Jsoup.connect에서 예외가 발생해도 전체 수집이 중단되지 않고 처리된다")
    void ingest_handlesExceptionInCrawlAllPagesWithState() {
        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString()))
                    .thenThrow(new RuntimeException("연결 실패"));

            assertDoesNotThrow(() -> service.ingest());

            // 연결이 실패했으므로, 저장은 한 번도 발생하지 않아야 한다.
            verify(productsRepository, never()).save(any());
            verify(housingRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------------
    // 3. crawlAndSave() - success / skipped / failed 분기 커버
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("crawlAndSave() - 정상 행/컬럼 부족/잘못된 링크/저장 에러 등 다양한 분기를 처리한다")
    void crawlAndSave_variousBranches() throws Exception {
        // (1) 정상 행
        String html = """
            <html><body>
              <table>
                <tbody>
                  <!-- 컬럼 부족 행 (skip) -->
                  <tr>
                    <td>0</td><td>1</td><td>2</td>
                  </tr>
                  <!-- panId 없는 링크 (skip) -->
                  <tr>
                    <td>0</td><td>1</td>
                    <td><a href="https://apply.lh.or.kr/invalid">제목</a></td>
                    <td>서울</td><td>-</td>
                    <td>2024.11.01</td>
                    <td>2024.11.30</td>
                    <td>공고중</td>
                  </tr>
                  <!-- 정상 행 -->
                  <tr>
                    <td>0</td><td>1</td>
                    <td>
                      <a href="https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancInfo.do?panId=999">
                        LH 공고 타이틀2
                      </a>
                    </td>
                    <td>서울특별시 마포구</td><td>-</td>
                    <td>2024.10.01</td>
                    <td>2024.10.31</td>
                    <td>접수중</td>
                  </tr>
                  <!-- 저장 중 예외 발생 행 -->
                  <tr>
                    <td>0</td><td>1</td>
                    <td>
                      <a href="https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancInfo.do?panId=1000">
                        예외 행
                      </a>
                    </td>
                    <td>서울특별시 은평구</td><td>-</td>
                    <td>2024.09.01</td>
                    <td>2024.09.30</td>
                    <td>모집완료</td>
                  </tr>
                </tbody>
              </table>
            </body></html>
            """;

        Document doc = Jsoup.parse(html, "https://apply.lh.or.kr");
        Elements rows = doc.select("table tbody tr");

        // detailUrl 999 → 정상 저장
        when(productsRepository.findByDetailUrl(contains("panId=999")))
                .thenReturn(Optional.empty());

        when(productsRepository.save(any(Products.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(housingRepository.findByProduct(any(Products.class)))
                .thenReturn(Optional.empty());

        // 첫 번째 save는 정상, 두 번째 save에서 예외 던짐 (failed++)
        when(housingRepository.save(any(HousingAnnouncements.class)))
                .thenAnswer(new Answer<HousingAnnouncements>() {
                    int count = 0;
                    @Override
                    public HousingAnnouncements answer(InvocationOnMock invocation) {
                        count++;
                        if (count == 2) {
                            throw new RuntimeException("저장 실패");
                        }
                        return invocation.getArgument(0);
                    }
                });

        // private crawlAndSave 호출 (reflection)
        Method m = LhHousingIngestService.class.getDeclaredMethod(
                "crawlAndSave", Elements.class, String.class, String.class);
        m.setAccessible(true);

        assertDoesNotThrow(() ->
                m.invoke(service, rows, "임대주택", "https://apply.lh.or.kr/list")
        );

        // 정상 행 1건 이상 저장 시도했는지 검증
        verify(housingRepository, atLeast(1)).save(any(HousingAnnouncements.class));
    }

    // ---------------------------------------------------------------------
    // 4. helper: parseDate / hasPanId / safeCut / firstNonBlank 등
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("유틸 메서드 - parseDate/hasPanId/safeCut/firstNonBlank 동작 확인")
    void helperMethods_basicBehaviors() throws Exception {
        // parseDate
        Method parseDate = LhHousingIngestService.class.getDeclaredMethod("parseDate", String.class);
        parseDate.setAccessible(true);
        LocalDate d1 = (LocalDate) parseDate.invoke(service, "2024.11.01");
        LocalDate d2 = (LocalDate) parseDate.invoke(service, "invalid");
        assertThat(d1).isEqualTo(LocalDate.of(2024, 11, 1));
        assertThat(d2).isNull();

        // hasPanId
        Method hasPanId = LhHousingIngestService.class.getDeclaredMethod("hasPanId", String.class);
        hasPanId.setAccessible(true);
        assertThat((boolean) hasPanId.invoke(service,
                "https://...selectWrtancInfo.do?panId=123")).isTrue();
        assertThat((boolean) hasPanId.invoke(service,
                "https://...selectWrtancInfo.do?panId=")).isFalse();
        assertThat((boolean) hasPanId.invoke(service,
                "https://...selectWrtancInfo.do")).isFalse();

        // safeCut
        Method safeCut = LhHousingIngestService.class.getDeclaredMethod("safeCut", String.class, int.class);
        safeCut.setAccessible(true);
        assertThat((String) safeCut.invoke(service, "12345", 10)).isEqualTo("12345");
        assertThat((String) safeCut.invoke(service, "1234567890", 5)).isEqualTo("12345");
        assertThat((String) safeCut.invoke(service, null, 5)).isNull();

        // firstNonBlank
        Method firstNonBlank = LhHousingIngestService.class.getDeclaredMethod("firstNonBlank", String[].class);
        firstNonBlank.setAccessible(true);
        assertThat((String) firstNonBlank.invoke(service, (Object) new String[]{null, "", "  ", "ok", "later"}))
                .isEqualTo("ok");
        assertThat((String) firstNonBlank.invoke(service, (Object) new String[]{null, "", "  "}))
                .isNull();
    }

    // ---------------------------------------------------------------------
    // 5. helper: mapToStatus / mapToCategory
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("유틸 메서드 - mapToStatus/mapToCategory enum 매핑 확인")
    void helperMethods_statusAndCategoryMapping() throws Exception {
        Method mapToStatus = LhHousingIngestService.class.getDeclaredMethod("mapToStatus", String.class);
        Method mapToCategory = LhHousingIngestService.class.getDeclaredMethod("mapToCategory", String.class);
        mapToStatus.setAccessible(true);
        mapToCategory.setAccessible(true);

        assertThat((HousingStatus) mapToStatus.invoke(service, "공고중"))
                .isEqualTo(HousingStatus.공고중);
        assertThat((HousingStatus) mapToStatus.invoke(service, "접수중"))
                .isEqualTo(HousingStatus.접수중);
        assertThat((HousingStatus) mapToStatus.invoke(service, "정정공고중"))
                .isEqualTo(HousingStatus.정정공고중);
        assertThat((HousingStatus) mapToStatus.invoke(service, "접수마감"))
                .isEqualTo(HousingStatus.접수마감);
        assertThat((HousingStatus) mapToStatus.invoke(service, "모집완료"))
                .isEqualTo(HousingStatus.모집완료);
        assertThat((HousingStatus) mapToStatus.invoke(service, "알수없음"))
                .isEqualTo(HousingStatus.공고중); // default

        assertThat((HousingCategory) mapToCategory.invoke(service, "임대주택"))
                .isEqualTo(HousingCategory.임대주택);
        assertThat((HousingCategory) mapToCategory.invoke(service, "분양주택"))
                .isEqualTo(HousingCategory.분양주택);
        assertThat((HousingCategory) mapToCategory.invoke(service, "상가"))
                .isEqualTo(HousingCategory.상가);
        assertThat((HousingCategory) mapToCategory.invoke(service, "토지"))
                .isEqualTo(HousingCategory.토지);
        assertThat((HousingCategory) mapToCategory.invoke(service, "주거복지"))
                .isEqualTo(HousingCategory.주거복지);
        assertThat((HousingCategory) mapToCategory.invoke(service, "기타등등"))
                .isEqualTo(HousingCategory.기타);
    }

    // ---------------------------------------------------------------------
    // 6. helper: extractTotalCount / resolveLastPage
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("resolveLastPage() - 페이지네이션/총건수 텍스트 기반 마지막 페이지 계산")
    void helperMethods_resolveLastPageAndExtractTotalCount() throws Exception {
        String html = """
            <html><body>
              <div class="paging">
                <a href="?pageIndex=1">1</a>
                <a href="?pageIndex=2">2</a>
                <a href="?pageIndex=3">3</a>
              </div>
              <div>총 120건</div>
              <table>
                <tbody>
                  <tr><td>row1</td></tr>
                  <tr><td>row2</td></tr>
                  <tr><td>row3</td></tr>
                  <tr><td>row4</td></tr>
                </tbody>
              </table>
            </body></html>
            """;

        Document doc = Jsoup.parse(html);

        Method extractTotalCount = LhHousingIngestService.class.getDeclaredMethod("extractTotalCount", Document.class);
        Method resolveLastPage = LhHousingIngestService.class.getDeclaredMethod("resolveLastPage", Document.class);
        extractTotalCount.setAccessible(true);
        resolveLastPage.setAccessible(true);

        int total = (int) extractTotalCount.invoke(service, doc);
        int lastPage = (int) resolveLastPage.invoke(service, doc);

        assertThat(total).isEqualTo(120);
        // 총 120건 / 페이지당 4행 → 30페이지, a 태그 최대 3이므로 30이 최종값
        assertThat(lastPage).isEqualTo(30);
    }

    // ---------------------------------------------------------------------
    // 7. helper: extractParamsFromOnclick / buildDetailUrl / resolveDetailUrl
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("resolveDetailUrl() - data-id / onclick / href 등 다양한 패턴으로 상세 URL을 만든다")
    void helperMethods_resolveDetailUrlAndBuildDetailUrl() throws Exception {
        // (1) onclick("goView('pan','ais','upp','ccr')") 패턴
        String html = """
            <html><body>
              <table><tbody>
                <tr id="row1">
                  <td>
                    <button onclick="goView('PAN123','AIS01','UPP02','CCR03')">보기</button>
                  </td>
                  <td>서울</td>
                  <td>2024.01.01</td>
                  <td>2024.01.31</td>
                  <td>공고중</td>
                </tr>
              </tbody></table>
            </body></html>
            """;

        Document doc = Jsoup.parse(html, "https://apply.lh.or.kr");
        Element row = doc.selectFirst("tr#row1");

        Method extractParamsFromOnclick = LhHousingIngestService.class.getDeclaredMethod(
                "extractParamsFromOnclick", String.class);
        extractParamsFromOnclick.setAccessible(true);

        Map<String, String> params = (Map<String, String>)
                extractParamsFromOnclick.invoke(service, row.selectFirst("button").attr("onclick"));

        assertThat(params.get("panId")).isEqualTo("PAN123");
        assertThat(params.get("aisTpCd")).isEqualTo("AIS01");
        assertThat(params.get("uppAisTpCd")).isEqualTo("UPP02");
        assertThat(params.get("ccrCnntSysDsCd")).isEqualTo("CCR03");

        // buildDetailUrl
        Method buildDetailUrl = LhHousingIngestService.class.getDeclaredMethod(
                "buildDetailUrl", Map.class, String.class);
        buildDetailUrl.setAccessible(true);

        String detailUrl = (String) buildDetailUrl.invoke(
                service, params,
                "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1026"
        );
        assertThat(detailUrl).contains("panId=PAN123");
        assertThat(detailUrl).contains("mi=1026");

        // panId가 비어 있으면 null
        params.put("panId", "");
        String nullUrl = (String) buildDetailUrl.invoke(
                service, params,
                "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1026"
        );
        assertThat(nullUrl).isNull();

        // resolveDetailUrl() - 마지막 fallback (a[href*='panId='])
        String html2 = """
            <html><body>
              <table><tbody>
                <tr id="row2">
                  <td>
                    <a href="https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancInfo.do?panId=555">
                      링크
                    </a>
                  </td>
                  <td>서울</td>
                  <td>2024.02.01</td>
                  <td>2024.02.28</td>
                  <td>공고중</td>
                </tr>
              </tbody></table>
            </body></html>
            """;

        Document doc2 = Jsoup.parse(html2, "https://apply.lh.or.kr");
        Element row2 = doc2.selectFirst("tr#row2");

        Method resolveDetailUrl = LhHousingIngestService.class.getDeclaredMethod(
                "resolveDetailUrl", Element.class, String.class);
        resolveDetailUrl.setAccessible(true);

        String resolved = (String) resolveDetailUrl.invoke(
                service,
                row2,
                "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1027"
        );

        assertThat(resolved).contains("panId=555");
    }

    // ---------------------------------------------------------------------
    // 8. tryAllPagingStrategies() - POST 전략 성공 케이스
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("tryAllPagingStrategies() - 첫 번째 POST 전략에서 행이 존재하면 즉시 해당 문서를 반환한다")
    void tryAllPagingStrategies_postStrategySuccess() throws Exception {
        String html = """
            <html><body>
              <table><tbody>
                <tr><td>0</td><td>1</td><td>제목</td><td>서울</td><td>-</td><td>2024.01.01</td><td>2024.01.31</td><td>공고중</td></tr>
              </tbody></table>
            </body></html>
            """;
        Document doc = Jsoup.parse(html);

        Connection connMock = mock(Connection.class);
        Connection.Response resMock = mock(Connection.Response.class);

        when(connMock.userAgent(anyString())).thenReturn(connMock);
        when(connMock.referrer(anyString())).thenReturn(connMock);
        when(connMock.timeout(anyInt())).thenReturn(connMock);
        when(connMock.method(any(Connection.Method.class))).thenReturn(connMock);
        when(connMock.followRedirects(anyBoolean())).thenReturn(connMock);
        when(connMock.cookies(anyMap())).thenReturn(connMock);
        when(connMock.header(anyString(), anyString())).thenReturn(connMock);
        when(connMock.data(anyString(), anyString())).thenReturn(connMock);
        when(connMock.execute()).thenReturn(resMock);
        when(resMock.cookies()).thenReturn(new HashMap<>());
        when(resMock.parse()).thenReturn(doc);

        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString()))
                    .thenReturn(connMock);

            Method m = LhHousingIngestService.class.getDeclaredMethod(
                    "tryAllPagingStrategies",
                    String.class, Map.class, Map.class, Map.class, int.class
            );
            m.setAccessible(true);

            Document result = (Document) m.invoke(
                    service,
                    "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1026",
                    new HashMap<>(),
                    new HashMap<>(),
                    new HashMap<>(),
                    2
            );

            assertThat(result.select("table tbody tr")).hasSize(1);
        }
    }

    // 추가 테스트 코드

    @Test
    @DisplayName("crawlAllPagesWithState - 2페이지에서 rows가 비어 있으면 '모든 전략 실패 → 수집 중단' 분기가 실행된다")
    void crawlAllPagesWithState_logsAllStrategiesFailAndBreaks() throws Exception {
        // page=1: '총 2건' 텍스트만 있고 행은 0개 → lastPage = 2
        String firstHtml = """
        <html><body>
          <div>총 2건</div>
          <table><tbody></tbody></table>
        </body></html>
        """;
        Document firstDoc = Jsoup.parse(firstHtml);

        // page=2: 완전히 빈 테이블 (rows.isEmpty() == true)
        String secondHtml = """
        <html><body>
          <table><tbody></tbody></table>
        </body></html>
        """;
        Document emptyDoc = Jsoup.parse(secondHtml);

        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            AtomicInteger callCount = new AtomicInteger();

            jsoup.when(() -> Jsoup.connect(anyString()))
                    .thenAnswer(invocation -> {
                        int n = callCount.getAndIncrement();

                        org.jsoup.Connection conn = mock(org.jsoup.Connection.class);
                        org.jsoup.Connection.Response res = mock(org.jsoup.Connection.Response.class);

                        when(conn.userAgent(anyString())).thenReturn(conn);
                        when(conn.referrer(anyString())).thenReturn(conn);
                        when(conn.timeout(anyInt())).thenReturn(conn);
                        when(conn.method(any(org.jsoup.Connection.Method.class))).thenReturn(conn);
                        when(conn.followRedirects(anyBoolean())).thenReturn(conn);
                        when(conn.header(anyString(), anyString())).thenReturn(conn);
                        when(conn.data(anyString(), anyString())).thenReturn(conn);
                        when(conn.cookies(anyMap())).thenReturn(conn);
                        when(conn.execute()).thenReturn(res);
                        when(res.cookies()).thenReturn(new HashMap<>());

                        // 0번째 호출: 첫 페이지(페이지 범위 계산용)
                        // 그 이후 호출(POST/GET/ fallback 전부): 빈 페이지
                        when(res.parse()).thenReturn(n == 0 ? firstDoc : emptyDoc);

                        return conn;
                    });

            // private void crawlAllPagesWithState(String url, String category) 호출
            Method m = LhHousingIngestService.class.getDeclaredMethod(
                    "crawlAllPagesWithState", String.class, String.class);
            m.setAccessible(true);

            assertDoesNotThrow(() ->
                    m.invoke(
                            service,
                            "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1026",
                            "임대주택"
                    )
            );
        }

        // rows가 계속 비어서 실제 DB save는 안 일어나야 정상
        verifyNoInteractions(productsRepository);
        verifyNoInteractions(housingRepository);
    }

    @Test
    @DisplayName("tryAllPagingStrategies - POST에서는 행이 없고 GET에서 행이 있으면 GET 결과를 반환한다")
    void tryAllPagingStrategies_usesGetWhenPostHasNoRows() throws Exception {
        String htmlWithRows = """
        <html><body>
          <table><tbody>
            <tr><td>row</td></tr>
          </tbody></table>
        </body></html>
        """;
        String htmlEmpty = """
        <html><body>
          <table><tbody></tbody></table>
        </body></html>
        """;

        Document docWithRows = Jsoup.parse(htmlWithRows);
        Document docEmpty = Jsoup.parse(htmlEmpty);

        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString()))
                    .thenAnswer(invocation -> {
                        String url = invocation.getArgument(0);
                        Connection conn = mock(Connection.class);
                        Connection.Response res = mock(Connection.Response.class);

                        when(conn.userAgent(anyString())).thenReturn(conn);
                        when(conn.referrer(anyString())).thenReturn(conn);
                        when(conn.timeout(anyInt())).thenReturn(conn);
                        when(conn.method(any(Connection.Method.class))).thenReturn(conn);
                        when(conn.followRedirects(anyBoolean())).thenReturn(conn);
                        when(conn.cookies(anyMap())).thenReturn(conn);
                        when(conn.header(anyString(), anyString())).thenReturn(conn);
                        when(conn.data(anyString(), anyString())).thenReturn(conn);
                        when(conn.execute()).thenReturn(res);
                        when(res.cookies()).thenReturn(new HashMap<>());

                        // GET 시도 (url에 currPage= / pageIndex= / pageNo= 포함) 일 때만 행이 있는 문서 반환
                        if (url.contains("currPage=") || url.contains("pageIndex=") || url.contains("pageNo=")) {
                            when(res.parse()).thenReturn(docWithRows);
                        } else {
                            // POST 전략들은 모두 빈 문서
                            when(res.parse()).thenReturn(docEmpty);
                        }

                        return conn;
                    });

            Method m = LhHousingIngestService.class.getDeclaredMethod(
                    "tryAllPagingStrategies",
                    String.class, Map.class, Map.class, Map.class, int.class);
            m.setAccessible(true);

            Document result = (Document) m.invoke(
                    service,
                    "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1026",
                    new HashMap<>(),
                    new HashMap<>(),
                    new HashMap<>(),
                    2
            );

            assertThat(result.select("table tbody tr")).hasSize(1); // GET 문서에서 온 결과
        }
    }

    @Test
    @DisplayName("tryAllPagingStrategies - POST/GET 모두 행이 없으면 fallback POST 결과를 반환한다")
    void tryAllPagingStrategies_fallbackWhenNoStrategyHasRows() throws Exception {
        String htmlEmpty = """
        <html><body>
          <table><tbody></tbody></table>
        </body></html>
        """;
        Document docEmpty = Jsoup.parse(htmlEmpty);

        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString()))
                    .thenAnswer(invocation -> {
                        Connection conn = mock(Connection.class);
                        Connection.Response res = mock(Connection.Response.class);

                        when(conn.userAgent(anyString())).thenReturn(conn);
                        when(conn.referrer(anyString())).thenReturn(conn);
                        when(conn.timeout(anyInt())).thenReturn(conn);
                        when(conn.method(any(Connection.Method.class))).thenReturn(conn);
                        when(conn.followRedirects(anyBoolean())).thenReturn(conn);
                        when(conn.cookies(anyMap())).thenReturn(conn);
                        when(conn.header(anyString(), anyString())).thenReturn(conn);
                        when(conn.data(anyString(), anyString())).thenReturn(conn);
                        when(conn.execute()).thenReturn(res);
                        when(res.cookies()).thenReturn(new HashMap<>());
                        when(res.parse()).thenReturn(docEmpty); // 항상 빈 문서

                        return conn;
                    });

            Method m = LhHousingIngestService.class.getDeclaredMethod(
                    "tryAllPagingStrategies",
                    String.class, Map.class, Map.class, Map.class, int.class);
            m.setAccessible(true);

            Document result = (Document) m.invoke(
                    service,
                    "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1026",
                    new HashMap<>(),
                    new HashMap<>(),
                    new HashMap<>(),
                    2
            );

            // fallback까지 갔다 온 결과(행은 없지만 null은 아니어야 함)
            assertThat(result).isNotNull();
            assertThat(result.select("table tbody tr")).isEmpty();
        }
    }

    @Test
    @DisplayName("resolveLastPage() - onclick 기반 goPage/selectPage/goPaging에서도 마지막 페이지를 추출한다")
    void resolveLastPage_usesOnclickBasedPagination() throws Exception {
        String html = """
        <html><body>
          <div class="paging">
            <a href="#" onclick="goPage('2')">2</a>
            <a href="#" onclick="goPage('5')">다음</a>
          </div>
          <table><tbody>
            <tr><td>row</td></tr>
          </tbody></table>
        </body></html>
        """;

        Document doc = Jsoup.parse(html);

        Method resolveLastPage = LhHousingIngestService.class.getDeclaredMethod("resolveLastPage", Document.class);
        resolveLastPage.setAccessible(true);

        int lastPage = (int) resolveLastPage.invoke(service, doc);
        assertThat(lastPage).isEqualTo(5); // onclick("goPage('5')")에서 5를 뽑아와야 함
    }

    @Test
    @DisplayName("crawlAndSave() - 같은 detailUrl은 seenDetailUrls로 한 번만 처리된다")
    void crawlAndSave_skipsDuplicateDetailUrls() throws Exception {
        String html = """
        <html><body>
          <table><tbody>
            <tr>
              <td>0</td><td>1</td>
              <td><a href="https://apply.lh.or.kr/...selectWrtancInfo.do?panId=777">공고1</a></td>
              <td>서울</td><td>-</td>
              <td>2024.11.01</td>
              <td>2024.11.30</td>
              <td>공고중</td>
            </tr>
            <tr>
              <td>0</td><td>1</td>
              <td><a href="https://apply.lh.or.kr/...selectWrtancInfo.do?panId=777">공고1-중복</a></td>
              <td>서울</td><td>-</td>
              <td>2024.11.01</td>
              <td>2024.11.30</td>
              <td>공고중</td>
            </tr>
          </tbody></table>
        </body></html>
        """;

        Document doc = Jsoup.parse(html, "https://apply.lh.or.kr");
        Elements rows = doc.select("table tbody tr");

        when(productsRepository.findByDetailUrl(anyString()))
                .thenReturn(Optional.empty());

        when(productsRepository.save(any(Products.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(housingRepository.findByProduct(any(Products.class)))
                .thenReturn(Optional.empty());

        when(housingRepository.save(any(HousingAnnouncements.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Method m = LhHousingIngestService.class.getDeclaredMethod(
                "crawlAndSave", Elements.class, String.class, String.class);
        m.setAccessible(true);

        assertDoesNotThrow(() ->
                m.invoke(service, rows, "임대주택",
                        "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1026")
        );

        // 같은 detailUrl이므로 Products/HousingAnnouncements는 각각 한 번만 저장
        verify(productsRepository, times(1)).save(any(Products.class));
        verify(housingRepository, times(1)).save(any(HousingAnnouncements.class));
    }

    @Test
    @DisplayName("crawlAndSave() - detailUrl이 이미 존재하면 기존 Products를 재사용한다")
    void crawlAndSave_usesExistingProductWhenPresent() throws Exception {
        String html = """
        <html><body>
          <table><tbody>
            <tr>
              <td>0</td><td>1</td>
              <td><a href="https://apply.lh.or.kr/...selectWrtancInfo.do?panId=888">기존공고</a></td>
              <td>서울</td><td>-</td>
              <td>2024.11.01</td>
              <td>2024.11.30</td>
              <td>접수중</td>
            </tr>
          </tbody></table>
        </body></html>
        """;

        Document doc = Jsoup.parse(html, "https://apply.lh.or.kr");
        Elements rows = doc.select("table tbody tr");

        Products existingProduct = new Products();
        existingProduct.setDetailUrl("https://apply.lh.or.kr/...selectWrtancInfo.do?panId=888");

        when(productsRepository.findByDetailUrl(anyString()))
                .thenReturn(Optional.of(existingProduct));

        HousingAnnouncements existingHa = new HousingAnnouncements(existingProduct);
        when(housingRepository.findByProduct(existingProduct))
                .thenReturn(Optional.of(existingHa));

        when(housingRepository.save(any(HousingAnnouncements.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Method m = LhHousingIngestService.class.getDeclaredMethod(
                "crawlAndSave", Elements.class, String.class, String.class);
        m.setAccessible(true);

        assertDoesNotThrow(() ->
                m.invoke(service, rows, "분양주택",
                        "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1027")
        );

        // 기존 Product를 써야 하므로 새 Products.save는 호출되면 안 됨
        verify(productsRepository, never()).save(any(Products.class));
        verify(housingRepository, times(1)).save(any(HousingAnnouncements.class));
    }

    @Test
    @DisplayName("resolveDetailUrl() - data-id / data-panid / hidden input 기반 상세 URL 생성")
    void resolveDetailUrl_handlesDataAttributesAndHiddenInputs() throws Exception {
        String html = """
        <html><body>
          <table><tbody>
            <!-- 1) .wrtancInfoBtn data-id1..4 -->
            <tr id="r1">
              <td>
                <button class="wrtancInfoBtn"
                        data-id1="PAN1"
                        data-id2="AIS1"
                        data-id3="UPP1"
                        data-id4="CCR1">
                  보기
                </button>
              </td>
            </tr>
            <!-- 3) data-panid / panId 속성 -->
            <tr id="r2">
              <td>
                <div data-panid="PAN2"
                     data-ais="AIS2"
                     data-upp="UPP2"
                     data-ccr="CCR2"></div>
              </td>
            </tr>
            <!-- 4) hidden input panId 류 -->
            <tr id="r3">
              <td>
                <input type="hidden" name="panId" value="PAN3"/>
                <input type="hidden" name="aisTpCd" value="AIS3"/>
                <input type="hidden" name="uppAisTpCd" value="UPP3"/>
                <input type="hidden" name="ccrCnntSysDsCd" value="CCR3"/>
              </td>
            </tr>
          </tbody></table>
        </body></html>
        """;

        Document doc = Jsoup.parse(html, "https://apply.lh.or.kr");
        Element row1 = doc.selectFirst("tr#r1");
        Element row2 = doc.selectFirst("tr#r2");
        Element row3 = doc.selectFirst("tr#r3");

        Method resolveDetailUrl = LhHousingIngestService.class.getDeclaredMethod(
                "resolveDetailUrl", Element.class, String.class);
        resolveDetailUrl.setAccessible(true);

        String listUrl = "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1026";

        String url1 = (String) resolveDetailUrl.invoke(service, row1, listUrl);
        assertThat(url1)
                .contains("panId=PAN1")
                .contains("aisTpCd=AIS1")
                .contains("uppAisTpCd=UPP1")
                .contains("ccrCnntSysDsCd=CCR1");

        String url2 = (String) resolveDetailUrl.invoke(service, row2, listUrl);
        assertThat(url2)
                .contains("panId=PAN2")
                .contains("aisTpCd=AIS2")
                .contains("uppAisTpCd=UPP2")
                .contains("ccrCnntSysDsCd=CCR2");

        String url3 = (String) resolveDetailUrl.invoke(service, row3, listUrl);
        assertThat(url3)
                .contains("panId=PAN3")
                .contains("aisTpCd=AIS3")
                .contains("uppAisTpCd=UPP3")
                .contains("ccrCnntSysDsCd=CCR3");
    }

    @Test
    @DisplayName("resolveDetailUrl() - onclick(goView(...)) 패턴에서 상세 URL을 생성한다")
    void resolveDetailUrl_usesOnclickPattern() throws Exception {
        String html = """
        <html><body>
          <table><tbody>
            <tr id="row">
              <td>
                <a href="#"
                   onclick="goView('PAN_ON', 'AIS_ON', 'UPP_ON', 'CCR_ON')">
                  보기
                </a>
              </td>
            </tr>
          </tbody></table>
        </body></html>
        """;

        // baseUrl에 mi=1026 포함 → buildDetailUrl()이 임대 페이지용 mi값 사용
        String listUrl = "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1026";

        Document doc = Jsoup.parse(html, "https://apply.lh.or.kr");
        Element row = doc.selectFirst("tr#row");

        Method m = LhHousingIngestService.class.getDeclaredMethod(
                "resolveDetailUrl", Element.class, String.class);
        m.setAccessible(true);

        String detailUrl = (String) m.invoke(service, row, listUrl);

        assertThat(detailUrl).isNotNull();
        assertThat(detailUrl)
                .contains("panId=PAN_ON")
                .contains("aisTpCd=AIS_ON")
                .contains("uppAisTpCd=UPP_ON")
                .contains("ccrCnntSysDsCd=CCR_ON")
                .contains("mi=1026");
    }
}
