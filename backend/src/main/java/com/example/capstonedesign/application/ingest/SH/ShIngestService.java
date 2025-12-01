package com.example.capstonedesign.application.ingest.SH;

import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import com.example.capstonedesign.domain.shannouncements.entity.RecruitStatus;
import com.example.capstonedesign.domain.shannouncements.entity.SHHousingCategory;
import com.example.capstonedesign.domain.shannouncements.entity.ShAnnouncement;
import com.example.capstonedesign.domain.shannouncements.repository.ShAnnouncementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ShIngestService
 * - 서울주택도시공사(i-SH) 공고(임대/분양) 데이터 크롤러
 * - Jsoup을 이용해 목록 및 상세 페이지를 수집하고 DB에 upsert 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShIngestService {

    private final ShAnnouncementRepository repo;
    private final ProductsRepository productsRepository;

    private static final String BASE = "https://www.i-sh.co.kr";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 임대 공급유형 코드 매핑 */
    private static final Map<String, String> SUPPLY_TYPES_RENT = Map.ofEntries(
            Map.entry("10", "청년안심주택"),
            Map.entry("07", "행복주택"),
            Map.entry("12", "사회주택"),
            Map.entry("11", "두레주택"),
            Map.entry("13", "도시형생활주택"),
            Map.entry("05", "장기안심주택"),
            Map.entry("04", "매입임대주택")
    );

    /** 분양 공급유형 코드 매핑 */
    private static final Map<String, String> SUPPLY_TYPES_SALE = Map.ofEntries(
            Map.entry("01", "일반분양"),
            Map.entry("02", "신혼희망타운"),
            Map.entry("03", "특별공급"),
            Map.entry("04", "공공분양"),
            Map.entry("05", "토지분양")
    );

    /** 서울 지역명 패턴 */
    private static final List<String> SEOUL_REGIONS = List.of(
            "강남", "강동", "강북", "강서", "관악", "광진", "구로", "금천",
            "노원", "도봉", "동대문", "동작", "마포", "서대문", "서초", "성동",
            "성북", "송파", "양천", "영등포", "용산", "은평", "종로", "중구", "중랑"
    );

    /** 공고 제목에서 지역 추출 */
    private String extractRegion(String title) {
        return SEOUL_REGIONS.stream()
                .filter(title::contains)
                .findFirst()
                .orElse("서울");
    }

    private static final String STATUS = "now"; // 진행 중 상태만 크롤링
    private static final int MAX_PAGES = 3;     // 페이지 제한

    /** 전체(임대 + 분양) 크롤링 실행 */
    public void crawlAll() {
        log.info("🚀 SH 공사 임대/분양 공고 크롤링 시작");

        crawlType("주택임대", "/main/lay2/program/S1T297C4476/www/brd/m_247/list.do", "2", SUPPLY_TYPES_RENT);
        crawlType("주택분양", "/main/lay2/program/S1T294C296/www/brd/m_244/list.do", "1", SUPPLY_TYPES_SALE);

        log.info("✅ SH 공사 임대/분양 공고 크롤링 완료");
    }

    /**
     * 공고 유형별(임대/분양) 페이지 크롤링
     */
    private void crawlType(String category, String path, String multiSeq, Map<String, String> supplyMap) {
        for (String splyTy : supplyMap.keySet()) {
            log.info("🏡 [{}] {} ({}) 진행중 공고 수집", category, supplyMap.get(splyTy), splyTy);
            try {
                for (int page = 1; page <= MAX_PAGES; page++) {
                    // 목록 페이지 요청
                    Document doc = Jsoup.connect(BASE + path)
                            .timeout(15000)
                            .userAgent("YouthCrawler/1.0")
                            .data("page", String.valueOf(page))
                            .data("multi_itm_seq", multiSeq)
                            .data("splyTy", splyTy)
                            .data("recrnotiState", STATUS)
                            .method(Connection.Method.POST)
                            .get();

                    Elements rows = doc.select("#listTb tbody tr");
                    if (rows.isEmpty()) break;

                    // 행별 데이터 파싱
                    for (Element tr : rows) {
                        Element a = tr.selectFirst("td.txtL a[onclick*=getDetailView]");
                        if (a == null) continue;

                        String title = a.text().trim();
                        String onclick = a.attr("onclick");
                        String externalId = extractSeq(onclick);
                        String dept = tr.select("td").get(2).text();
                        String postDate = tr.select("td.num").get(0).text();
                        String views = tr.select("td.num").get(1).text();

                        // 상세 URL 생성
                        String detailUrl = BASE + path.replace("list.do", "view.do")
                                + "?seq=" + externalId
                                + "&multi_itm_seq=" + multiSeq;

                        // 상세 페이지 요청
                        Document detail = Jsoup.connect(detailUrl)
                                .timeout(15000)
                                .userAgent("YouthCrawler/1.0")
                                .method(Connection.Method.GET)
                                .get();

                        // 본문 및 첨부파일 추출
                        Element content = detail.selectFirst(".board_view, .viewCont, #contents");
                        String html = content != null ? content.outerHtml() : "";

                        List<Map<String, String>> files = new ArrayList<>();
                        for (Element f : detail.select(".attach a, .file a, .down a")) {
                            files.add(Map.of(
                                    "name", f.text(),
                                    "url", BASE + f.attr("href")
                            ));
                        }

                        // ==============================================
                        // 1) PRODUCT 먼저 생성
                        // ==============================================
                        Products product = productsRepository.save(
                                Products.builder()
                                        .type(ProductType.HOUSING)
                                        .name(title)
                                        .provider("SH 서울주택도시공사")
                                        .detailUrl(detailUrl)
                                        .build()
                        );

                        // ==============================================
                        // 2) SH 공고 생성 + product 매핑
                        // ==============================================
                        ShAnnouncement ann = ShAnnouncement.builder()
                                .product(product)
                                .source("i-sh")
                                .externalId(externalId)
                                .title(title)
                                .department(dept)
                                .postDate(parseDate(postDate))
                                .views(parseInt(views))
                                .recruitStatus(RecruitStatus.now)
                                .supplyType(supplyMap.get(splyTy))
                                .category(SHHousingCategory.valueOf(category))
                                .region(extractRegion(title))
                                .contentHtml(html)
                                .attachments(toJson(files))
                                .detailUrl(detailUrl)
                                .crawledAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                        upsert(ann);
                        Thread.sleep(700);
                    }
                }
            } catch (Exception e) {
                log.error("❌ [{}] {} 크롤링 실패: {}", category, supplyMap.get(splyTy), e.getMessage());
            }
        }
    }

    /** 기존 데이터는 업데이트, 없으면 신규 저장 */
    private void upsert(ShAnnouncement a) {

        repo.findBySourceAndExternalId(a.getSource(), a.getExternalId())
                .ifPresentOrElse(e -> {
                    Products product = e.getProduct();

                    e.setProduct(product);   // 중요
                    e.setTitle(a.getTitle());
                    e.setDepartment(a.getDepartment());
                    e.setPostDate(a.getPostDate());
                    e.setViews(a.getViews());
                    e.setRecruitStatus(a.getRecruitStatus());
                    e.setSupplyType(a.getSupplyType());
                    e.setCategory(a.getCategory());
                    e.setRegion(a.getRegion());
                    e.setContentHtml(a.getContentHtml());
                    e.setAttachments(a.getAttachments());
                    e.setDetailUrl(a.getDetailUrl());
                    e.setUpdatedAt(LocalDateTime.now());

                    repo.save(e);
                }, () -> repo.save(a));
    }

    /** onclick 속성에서 seq 추출 */
    private String extractSeq(String js) {
        Matcher m = Pattern.compile("getDetailView\\('?(\\d+)'?\\)").matcher(js);
        return m.find() ? m.group(1) : "";
    }

    /** 날짜 파싱 */
    private LocalDate parseDate(String s) {
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return null; }
    }

    /** 숫자 파싱 */
    private Integer parseInt(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); } catch (Exception e) { return 0; }
    }

    /** 객체 → JSON 문자열 변환 */
    private String toJson(Object obj) {
        try { return MAPPER.writeValueAsString(obj); } catch (Exception e) { return "[]"; }
    }

    /** 프로젝트 전체 구조 통일용 Wrapper 메서드 */
    public void syncNotices() {
        crawlAll();
    }
}
