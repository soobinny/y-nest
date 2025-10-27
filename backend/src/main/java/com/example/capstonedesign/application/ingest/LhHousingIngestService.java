package com.example.capstonedesign.application.ingest;

import com.example.capstonedesign.domain.housingannouncements.entity.HousingAnnouncements;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingStatus;
import com.example.capstonedesign.domain.housingannouncements.repository.HousingAnnouncementsRepository;
import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LhHousingIngestService {

    private final ProductsRepository productsRepository;
    private final HousingAnnouncementsRepository housingRepository;

    /** LH 공고 목록/상세 베이스 URL 및 고정 파라미터 */
    private static final String BASE = "https://apply.lh.or.kr";
    private static final String RENT_URL = BASE + "/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1026"; // 임대
    private static final String SALE_URL = BASE + "/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1027"; // 분양
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127 Safari/537.36";

    /**
     * 스케줄러 진입점.
     * - 임대/분양 각각에 대해 페이징 전체 순회 + 목록 파싱 + 상세 URL 구성 → DB upsert
     */
    @Transactional
    public void ingest() {
        log.info("LH 공고 데이터 수집 시작");
        crawlAllPagesWithState(RENT_URL, "임대주택");
        crawlAllPagesWithState(SALE_URL, "분양주택");
        log.info("LH 공고 데이터 수집 완료");
    }

    /**
     * 주어진 목록 URL(임대/분양)에 대해 첫 페이지로 히든필드/쿠키 상태를 획득한 뒤,
     * 다양한 페이징 전략(POST/GET, 파라미터 이름 변화, Egov 패턴 등)을 시도하며 2페이지 이후를 순회한다.
     *
     * @param url       LH 목록 페이지(URL에 mi=1026/1027 포함)
     * @param category  "임대주택" | "분양주택" (도메인 매핑용)
     */
    private void crawlAllPagesWithState(String url, String category) {
        try {
            // 1) 첫 페이지는 보통 POST currPage=1로 진입해야 히든/쿠키가 정상 세팅됨
            org.jsoup.Connection firstConn = Jsoup.connect(url)
                    .userAgent(UA)
                    .referrer(BASE)
                    .timeout(20000)
                    .method(org.jsoup.Connection.Method.POST)
                    .followRedirects(true)
                    .header("Origin", BASE)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .data("currPage", "1"); // 페이지 인자: 사이트마다 명칭이 다름(currPage/pageIndex/pageNo 등)

            org.jsoup.Connection.Response firstRes = firstConn.execute();
            Document firstDoc = firstRes.parse();
            Map<String, String> cookies = new HashMap<>(firstRes.cookies()); // 쿠키 보존(세션성 페이징 사이트 대응)

            int lastPage = resolveLastPage(firstDoc); // 페이지네이션 UI/텍스트/총건수 기반 추정
            if (lastPage < 1) lastPage = 1;
            log.info("페이지 범위 확정: 1 ~ {}", lastPage);

            // 첫 페이지 처리
            Elements firstRows = firstDoc.select("table tbody tr");
            log.info("p=1 행수: {}", firstRows.size());
            if (!firstRows.isEmpty()) {
                crawlAndSave(firstRows, category, url);
            }

            // 히든 파라미터 캐시(페이징 폼/검색 폼 모두 시도)
            Map<String, String> pagingHidden = extractFormHidden(firstDoc, "pagingForm");
            Map<String, String> srchHidden   = extractFormHidden(firstDoc, "srchForm");

            // 2) 2..lastPage 순회 — 전략적으로 POST/GET 조합 시도
            for (int p = 2; p <= lastPage; p++) {
                Document doc = tryAllPagingStrategies(url, cookies, pagingHidden, srchHidden, p);
                Elements rows = doc.select("table tbody tr");

                if (rows.isEmpty()) {
                    // 어떤 전략으로도 rows를 못 얻었다면 구조 변경/차단 가능성 → 바로 중단(안전)
                    log.info("p={} 모든 전략 실패 → 수집 중단", p);
                    break;
                }

                log.info("p={} 행수: {}", p, rows.size());
                crawlAndSave(rows, category, url);

                // 다음 페이지에 대비해 최신 히든 필드로 갱신(서버가 매 페이지 바꾸는 경우가 있어 반영)
                Map<String, String> nextPaging = extractFormHidden(doc, "pagingForm");
                if (!nextPaging.isEmpty()) pagingHidden = nextPaging;

                // 너무 공격적인 요청 방지(간단한 예의 지연)
                Thread.sleep(700);
            }

        } catch (Exception e) {
            // 전체 수집 실패가 다른 카테고리에 영향 주지 않도록 내부에서 에러 처리 후 로그만 남김
            log.error("crawlAllPagesWithState 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 하나의 페이지를 가져오기 위해 여러 '페이징 파라미터 이름/방식'을 순차적으로 시도한다.
     * - 일부 공공 사이트는 pageIndex / pageNo / currPage / firstIndex / lastIndex 등 혼용
     * - POST가 기본이나, GET 쿼리스트링으로도 되는 경우가 있어 최후에 GET도 시도
     */
    private Document tryAllPagingStrategies(
            String url,
            Map<String, String> cookies,
            Map<String, String> pagingHidden,
            Map<String, String> srchHidden,
            int page
    ) throws Exception {

        List<Map<String, String>> candidates = new ArrayList<>();

        // 기준 히든 세트: pagingForm이 우선, 없으면 srchForm 사용
        Map<String, String> baseHidden = !pagingHidden.isEmpty() ? pagingHidden : srchHidden;

        // 페이지당 행 수(listCo/pageUnit/recordCountPerPage 등) 추정
        String listCoStr = baseHidden.getOrDefault("listCo",
                baseHidden.getOrDefault("pageUnit",
                        baseHidden.getOrDefault("recordCountPerPage", "50")));
        int listCo;
        try { listCo = Integer.parseInt(listCoStr); } catch (Exception ignore) { listCo = 50; }

        // Egov 및 내부 페이징에서 사용하는 인덱스 범위 계산
        int minSn = (page - 1) * listCo;
        int maxSn = page * listCo;

        // 전략 1) currPage 중심(가능하면 minSn/maxSn 포함)
        candidates.add(buildPageParams(baseHidden, page, "currPage", minSn, maxSn, true));
        // 전략 2) pageIndex
        candidates.add(buildPageParams(baseHidden, page, "pageIndex", minSn, maxSn, true));
        // 전략 3) pageNo
        candidates.add(buildPageParams(baseHidden, page, "pageNo", minSn, maxSn, true));
        // 전략 4) Egov 패턴(firstIndex/lastIndex/recordCountPerPage + pageIndex)
        Map<String, String> egov = new LinkedHashMap<>(baseHidden);
        egov.put("firstIndex", String.valueOf(minSn));
        egov.put("lastIndex", String.valueOf(maxSn));
        egov.putIfAbsent("recordCountPerPage", String.valueOf(listCo));
        egov.put("pageIndex", String.valueOf(page));
        candidates.add(egov);
        // 전략 5) 간소화(일부 사이트는 minSn/maxSn 있으면 실패) → 제거 버전
        candidates.add(buildPageParams(baseHidden, page, "currPage", minSn, maxSn, false));

        // ---- POST 시도(우선) ----
        for (int i = 0; i < candidates.size(); i++) {
            Map<String, String> params = candidates.get(i);
            Document doc = post(url, cookies, params, "POST#" + (i + 1));
            if (!doc.select("table tbody tr").isEmpty()) return doc; // 성공 기준: 테이블 행 존재
        }

        // ---- GET 시도(보조) ----
        String[] pageNames = {"currPage", "pageIndex", "pageNo"};
        for (String pn : pageNames) {
            String qs = pn + "=" + page;
            Document doc = get(url, cookies, qs, "GET#" + pn);
            if (!doc.select("table tbody tr").isEmpty()) return doc;
        }

        // 마지막 안전장치: 가장 첫 POST 조합 결과 반환(디버깅 로그 참고)
        return post(url, cookies, candidates.get(0), "POST#fallback");
    }

    /**
     * 페이징 요청 파라미터 조합을 생성.
     * - 동일 의미 다른 키(currPage/pageIndex/pageNo) 혼재 대비
     * - includeSn=false면 minSn/maxSn 제거하여 서버 검증 회피 시도
     */
    private Map<String, String> buildPageParams(Map<String, String> base, int page,
                                                String pageParam, int minSn, int maxSn,
                                                boolean includeSn) {
        Map<String, String> m = new LinkedHashMap<>(base);
        m.put(pageParam, String.valueOf(page));

        // 동명이인 파라미터 제거(서버가 한 가지만 기대하는 케이스 방지)
        if (!"currPage".equals(pageParam)) m.remove("currPage");
        if (!"pageIndex".equals(pageParam)) m.remove("pageIndex");
        if (!"pageNo".equals(pageParam)) m.remove("pageNo");

        if (includeSn) {
            // 해당 키가 존재할 때만 값 갱신(불필요한 추가를 피함)
            if (m.containsKey("minSn")) m.put("minSn", String.valueOf(minSn));
            if (m.containsKey("maxSn")) m.put("maxSn", String.valueOf(maxSn));
            if (m.containsKey("prevListCo")) m.put("prevListCo", m.getOrDefault("listCo", "50"));
        } else {
            // 서버 검증이 민감한 경우를 위해 인덱스 제거
            m.remove("minSn");
            m.remove("maxSn");
        }
        return m;
    }

    /** POST 요청 공통 유틸(디버그용 파라미터 로그 포함, 쿠키 업데이트) */
    private Document post(String url, Map<String, String> cookies,
                          Map<String, String> data, String tag) throws Exception {
        org.jsoup.Connection conn = Jsoup.connect(url)
                .userAgent(UA)
                .referrer(url) // 동일 페이지를 referrer로 주면 차단을 덜 받는 경우가 있음
                .timeout(20000)
                .method(org.jsoup.Connection.Method.POST)
                .followRedirects(true)
                .cookies(cookies)
                .header("Origin", BASE)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // null/blank 키/값 방어적으로 제외
        for (Map.Entry<String, String> e : data.entrySet()) {
            if (e.getKey() != null && !e.getKey().isBlank() && e.getValue() != null) {
                conn.data(e.getKey(), e.getValue());
            }
        }

        logDebugParams(tag, data);
        org.jsoup.Connection.Response res = conn.execute();
        cookies.putAll(res.cookies()); // 서버가 세션 쿠키를 갱신하는 패턴 대응
        return res.parse();
    }

    /** GET 요청 공통 유틸(간단한 쿼리스트링 페이징 지원) */
    private Document get(String url, Map<String, String> cookies, String qs, String tag) throws Exception {
        String getUrl = url + (url.contains("?") ? "&" : "?") + qs;
        log.info("{} → GET {}", tag, getUrl.replace(BASE, "")); // 내부 로그에서 호스트 생략(가독성)
        org.jsoup.Connection.Response res = Jsoup.connect(getUrl)
                .userAgent(UA)
                .referrer(url)
                .timeout(20000)
                .method(org.jsoup.Connection.Method.GET)
                .followRedirects(true)
                .cookies(cookies)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .execute();
        cookies.putAll(res.cookies());
        return res.parse();
    }

    /** 너무 긴 파라미터는 잘라서 로그에 출력(민감 정보/가독성) */
    private void logDebugParams(String tag, Map<String, String> data) {
        try {
            Map<String, String> print = new LinkedHashMap<>(data);
            print.replaceAll((k, v) -> v == null ? null : (v.length() > 80 ? v.substring(0, 77) + "..." : v));
            log.info("{} → POST params: {}", tag, print);
        } catch (Exception ignore) {}
    }

    /** form[name=?] 범위에서 hidden input[name]만 추출하여 map 으로 반환 */
    private Map<String, String> extractFormHidden(Document doc, String formName) {
        Map<String, String> map = new LinkedHashMap<>();
        Element form = doc.selectFirst("form[name=" + formName + "]");
        if (form == null) return map;
        for (Element input : form.select("input[type=hidden][name]")) {
            String name = input.attr("name");
            String val = input.attr("value");
            if (name != null && !name.isBlank()) map.put(name, val);
        }
        return map;
    }

    /**
     * 마지막 페이지 번호 추정.
     * - a[href]의 page 파라미터, onclick(goPage/...)의 인자, "총 N건" 텍스트를 조합
     * - 총건수/행수 기반 계산으로 UI 누락/버튼 비노출 케이스도 커버
     */
    private int resolveLastPage(Document doc) {
        int max = 1;

        // 1) 페이지네이션 a 태그에서 숫자/쿼리 파라미터 추출
        for (Element a : doc.select(".paginate a, .pagination a, .paging a, .bbs_pagerA a, a[href*=pageIndex], a[href*=currPage], a[href*=pageNo]")) {
            String t = a.text().trim();
            if (t.matches("\\d+")) max = Math.max(max, Integer.parseInt(t));
            String href = a.attr("href");
            Matcher m1 = Pattern.compile("(?:pageIndex|currPage|pageNo)=(\\d+)").matcher(href);
            while (m1.find()) max = Math.max(max, Integer.parseInt(m1.group(1)));
        }

        // 2) onclick 기반 페이징 (goPage/selectPage/goPaging)
        for (Element e : doc.select("*[onclick*=goPage], *[onclick*=selectPage], *[onclick*=goPaging]")) {
            Matcher m = Pattern.compile("(?:goPage|selectPage|goPaging)\\(['\\\"]?(\\d+)['\\\"]?\\)").matcher(e.attr("onclick"));
            while (m.find()) max = Math.max(max, Integer.parseInt(m.group(1)));
        }

        // 3) 총건수 텍스트 기반 보정 (행수로 나눠 ceiling)
        int total = extractTotalCount(doc);
        if (total > 0) {
            int rows1 = Math.max(1, doc.select("table tbody tr").size()); // 0으로 나눔 방지
            int byTotal = (int) Math.ceil(total / (double) rows1);
            max = Math.max(max, byTotal);
        }
        return max;
    }

    /** 문서 내 텍스트에서 "총 N건", "(N건)", "전체 N 건" 패턴을 찾아 정수로 반환 */
    private int extractTotalCount(Document doc) {
        String text = doc.text();
        Matcher m = Pattern.compile("총\\s*([0-9,]+)건|\\(([0-9,]+)건\\)|전체\\s*([0-9,]+)\\s*건").matcher(text);
        if (m.find()) {
            String g = m.group(1) != null ? m.group(1) : (m.group(2) != null ? m.group(2) : m.group(3));
            try { return Integer.parseInt(g.replace(",", "")); } catch (Exception ignored) {}
        }
        return -1;
    }

    /**
     * 목록 테이블의 각 행을 도메인 엔티티(Products/HousingAnnouncements)로 upsert.
     * - detailUrl을 key로 Products 찾고 없으면 생성
     * - HousingAnnouncements는 product FK 기준으로 생성/갱신
     * - 동일 페이지 중복 링크(seenDetailUrls) 제거로 삽입 중복 방지
     */
    private void crawlAndSave(Elements rows, String category, String url) {
        int success = 0, skipped = 0, failed = 0;
        int insertProducts = 0, insertHA = 0, updateHA = 0;
        Set<String> seenDetailUrls = new HashSet<>();

        for (Element tr : rows) {
            try {
                Elements tds = tr.select("td");
                if (tds.size() < 8) { // 컬럼 수 부족(머리글/빈행 등)
                    skipped++; continue;
                }

                // 컬럼 인덱스는 LH 테이블 현재 구조에 맞춤(구조 변경 시 수정 필요)
                String title = safeCut(tds.get(2).text().trim(), 150);
                String region = safeCut(tds.get(3).text().trim(), 100);
                String noticeDateStr = tds.get(5).text().trim();
                String closeDateStr  = tds.get(6).text().trim();
                String status        = tds.get(7).text().trim();

                LocalDate noticeDate = parseDate(noticeDateStr); // "yyyy.MM.dd" → LocalDate(파싱 실패 시 null 허용)
                LocalDate closeDate  = parseDate(closeDateStr);

                // 상세 이동 링크 구성(다양한 패턴: data-* / onclick / hidden / a[href] ...)
                String rawDetailUrl = resolveDetailUrl(tr, url);
                if (rawDetailUrl == null || rawDetailUrl.isBlank() || !hasPanId(rawDetailUrl)) {
                    // panId 없이 노출되는 가짜/비활성 링크는 스킵
                    skipped++; continue;
                }

                String detailUrl = safeCut(rawDetailUrl, 500);
                if (!seenDetailUrls.add(detailUrl)) { // 같은 페이지 내 중복 제거(탭/버튼 중복 등)
                    skipped++; continue;
                }

                // Products upsert (detailUrl을 유니크 키처럼 사용)
                Optional<Products> opt = productsRepository.findByDetailUrl(detailUrl);
                Products product;
                if (opt.isPresent()) {
                    product = opt.get();
                } else {
                    Products p = new Products();
                    p.setType(ProductType.HOUSING);
                    p.setName(title);
                    p.setProvider("LH");
                    p.setDetailUrl(detailUrl);
                    product = productsRepository.save(p);
                    insertProducts++;
                }

                // HousingAnnouncements upsert (product 기준 1:1 가정)
                final Products prodRef = product; // 람다/익명 클래스 대비 effectively final
                HousingAnnouncements ha = housingRepository.findByProduct(prodRef)
                        .orElseGet(() -> new HousingAnnouncements(prodRef));

                boolean isNew = (ha.getId() == null);
                ha.setRegionName(region);
                ha.setNoticeDate(noticeDate);
                ha.setCloseDate(closeDate);
                ha.setStatus(mapToStatus(status));          // 문자열 상태 → enum 매핑(미정인 경우 종료로 fallback)
                ha.setCategory(mapToCategory(category));    // "임대주택"/"분양주택" → enum

                housingRepository.save(ha);
                if (isNew) insertHA++; else updateHA++;
                success++;

            } catch (Exception ex) {
                failed++;
                // 문제 행의 텍스트를 같이 남겨 디버깅 용이성 확보
                log.error("행 처리 중 오류: {}", tr.text(), ex);
            }
        }

        log.info("[{}] 결과 요약: success={}, skipped={}, failed={}, insertProducts={}, insertHA={}, updateHA={}",
                category, success, skipped, failed, insertProducts, insertHA, updateHA);
    }

    // ------------------------- 유틸 ----------------------------

    /** "yyyy.MM.dd" 형식만 허용. 실패 시 null 반환(일부 공고는 빈 값 존재) */
    private LocalDate parseDate(String text) {
        try {
            if (text == null || text.isBlank()) return null;
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (Exception e) { return null; }
    }

    /** 상세 URL 내 panId 필수 검증(=상세 이동 가능한 진짜 링크) */
    private boolean hasPanId(String url) {
        return url != null && url.contains("panId=") && !url.matches(".*[?&]panId=(&|$)");
    }

    /** 문자열 길이 제한(칼럼 최대 길이 안전 확보) */
    private String safeCut(String s, int max) { if (s == null) return null; return (s.length() <= max) ? s : s.substring(0, max); }

    private String nvl(String s) { return (s == null) ? "" : s; }

    /** 여러 후보 중 첫 번째 non-blank 반환 */
    private String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    /** 행(row) 범위에서 hidden[name] 값 조회 */
    private String valueOfHidden(Element scope, String name) {
        Element input = scope.selectFirst("input[type=hidden][name=" + name + "]");
        return input != null ? input.attr("value") : null;
    }

    /**
     * onclick("goView('pan','ais','upp','ccr')") 형태에서 인자 추출.
     * - 따옴표 묶인 인자를 순서대로 매핑
     */
    private Map<String, String> extractParamsFromOnclick(String onclick) {
        Map<String, String> map = new HashMap<>();
        if (onclick == null || onclick.isBlank()) return map;
        Matcher m = Pattern.compile("['\\\"]([^'\\\"]*)['\\\"]").matcher(onclick);
        List<String> vals = new ArrayList<>();
        while (m.find()) vals.add(m.group(1));
        if (vals.size() >= 4) {
            map.put("panId", vals.get(0));
            map.put("aisTpCd", vals.get(1));
            map.put("uppAisTpCd", vals.get(2));
            map.put("ccrCnntSysDsCd", vals.get(3));
        }
        return map;
    }

    /**
     * 테이블 행에서 상세 이동을 위한 파라미터를 최대한 다양한 방식으로 추출하여 상세 URL을 구성.
     * 우선순위:
     *   1) .wrtancInfoBtn 의 data-id1..4
     *   2) onclick(goView/selectWrtancInfo/goDetail)
     *   3) data-panId / panId 유사 속성
     *   4) hidden panId 등
     *   5) a[href*='panId='] 직접 링크
     */
    private String resolveDetailUrl(Element row, String listUrl) {
        if (row == null) return null;

        // 1) 공식 버튼에 data-id1..4로 담기는 패턴
        Element lhBtn = row.selectFirst(".wrtancInfoBtn[data-id1][data-id2][data-id3][data-id4]");
        if (lhBtn != null) {
            Map<String, String> p = new HashMap<>();
            p.put("panId", lhBtn.attr("data-id1"));
            p.put("aisTpCd", lhBtn.attr("data-id2"));
            p.put("uppAisTpCd", lhBtn.attr("data-id3"));
            p.put("ccrCnntSysDsCd", lhBtn.attr("data-id4"));
            String built = buildDetailUrl(p, listUrl);
            if (built != null && hasPanId(built)) return built;
        }

        // 2) onclick 함수 인자에서 추출
        Element withOnclick = row.selectFirst("*[onclick*=\"goView\"], *[onclick*=\"selectWrtancInfo\"], *[onclick*=\"goDetail\"]");
        if (withOnclick != null) {
            Map<String, String> p = extractParamsFromOnclick(withOnclick.attr("onclick"));
            String built = buildDetailUrl(p, listUrl);
            if (built != null && hasPanId(built)) return built;
        }

        // 3) data-panId / panId 속성류
        Element withData = row.selectFirst("*[data-panid], *[data-pan-id], *[panid], *[panId]");
        if (withData != null) {
            Map<String, String> p = new HashMap<>();
            p.put("panId", firstNonBlank(withData.attr("data-panid"), withData.attr("data-pan-id"), withData.attr("panId"), withData.attr("panid")));
            p.put("aisTpCd", firstNonBlank(withData.attr("data-ais"), withData.attr("aisTpCd")));
            p.put("uppAisTpCd", firstNonBlank(withData.attr("data-upp"), withData.attr("uppAisTpCd")));
            p.put("ccrCnntSysDsCd", firstNonBlank(withData.attr("data-ccr"), withData.attr("ccrCnntSysDsCd")));
            String built = buildDetailUrl(p, listUrl);
            if (built != null && hasPanId(built)) return built;
        }

        // 4) hidden input 에서 panId 류
        String panFromHidden = firstNonBlank(valueOfHidden(row, "panId"), valueOfHidden(row, "panid"));
        if (panFromHidden != null && !panFromHidden.isBlank()) {
            Map<String, String> p = new HashMap<>();
            p.put("panId", panFromHidden);
            p.put("aisTpCd", firstNonBlank(valueOfHidden(row, "aisTpCd")));
            p.put("uppAisTpCd", firstNonBlank(valueOfHidden(row, "uppAisTpCd")));
            p.put("ccrCnntSysDsCd", firstNonBlank(valueOfHidden(row, "ccrCnntSysDsCd")));
            String built = buildDetailUrl(p, listUrl);
            if (built != null && hasPanId(built)) return built;
        }

        // 5) a[href] 직접 링크(마지막 수단)
        Element aWithHref = row.selectFirst("a[href*='panId=']");
        if (aWithHref != null) {
            String href = aWithHref.absUrl("href");
            if (href != null && hasPanId(href)) return href;
        }

        return null;
    }

    /**
     * 상세 페이지 이동 URL 구성.
     * - 목록 URL의 mi 값(임대/분양)을 유지하여 상세로 이동
     * - panId 없으면 null 반환(유효하지 않은 링크)
     */
    private String buildDetailUrl(Map<String, String> p, String listUrl) {
        String mi = listUrl.contains("mi=1026") ? "1026" : (listUrl.contains("mi=1027") ? "1027" : "");
        String panId = nvl(p.get("panId"));
        if (panId.isBlank()) return null;
        String ais = nvl(p.get("aisTpCd"));
        String upp = nvl(p.get("uppAisTpCd"));
        String ccr = nvl(p.get("ccrCnntSysDsCd"));

        return BASE + "/lhapply/apply/wt/wrtanc/selectWrtancInfo.do"
                + "?aisTpCd=" + ais
                + "&ccrCnntSysDsCd=" + ccr
                + "&mi=" + mi
                + "&panId=" + panId
                + "&uppAisTpCd=" + upp;
    }

    /** 사이트 표시 문자열을 도메인 enum 으로 매핑(알 수 없는 값은 종료로 폴백) */
    private HousingStatus mapToStatus(String status) {
        if (status == null) return HousingStatus.공고중;
        return switch (status.trim()) {
            case "공고중" -> HousingStatus.공고중;
            case "접수중" -> HousingStatus.접수중;
            case "정정공고중" -> HousingStatus.정정공고중;
            case "접수마감" -> HousingStatus.접수마감;
            default -> HousingStatus.종료; // 예외 케이스 안전 처리
        };
    }

    /** 카테고리 문자열을 도메인 enum 으로 매핑(기본값: 임대) */
    private HousingCategory mapToCategory(String category) {
        if (category == null) return HousingCategory.임대주택;
        return switch (category.trim()) {
            case "분양주택" -> HousingCategory.분양주택;
            default -> HousingCategory.임대주택;
        };
    }
}
