package com.example.capstonedesign.application.ingest.LH;

import com.example.capstonedesign.domain.housingannouncements.entity.LhNotice;
import com.example.capstonedesign.domain.housingannouncements.repository.LhNoticeRepository;
import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * LhLeaseNoticeService
 * ---------------------------------------------------------
 * - LH(한국토지주택공사) 임대공고 데이터를 공공데이터포털 API로부터 수집
 * - JSON 응답을 파싱하여 DB에 저장 (중복 방지)
 * - 공고명(panNm) + 게시일(panNtStDt) 기준으로 중복 체크
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LhLeaseNoticeService {

    /** LH 공고 Repository (DB 저장용) */
    private final LhNoticeRepository lhNoticeRepository;
    private final ProductsRepository productsRepository;

    /** 공공데이터포털 API 인증키 (application.yml에서 주입) */
    @Value("${lh.api.service-key}")
    private String serviceKey;

    /** LH 임대공고 API 기본 URL */
    private static final String BASE_URL =
            "https://apis.data.go.kr/B552555/lhLeaseNoticeInfo1/lhLeaseNoticeInfo1";

    /**
     * LH 임대공고 데이터 수집 메서드
     * -------------------------------------------------
     * - 페이지 단위로 API 호출
     * - JSON 파싱 후 신규 데이터만 DB 저장
     * - "공고중" 상태 데이터만 수집
     */
    public void fetchNotices() {
        int page = 1;
        int totalCount = 0;
        ObjectMapper mapper = new ObjectMapper();

        try {
            while (true) {
                // 한글 파라미터 인코딩 ("공고중")
                String panStatus = URLEncoder.encode("공고중", StandardCharsets.UTF_8);

                // API 요청 URL 구성
                String apiUrl = String.format(
                        "%s?ServiceKey=%s&PG_SZ=100&PAGE=%d&_type=json&PAN_SS=%s",
                        BASE_URL, serviceKey, page, panStatus
                );

                log.info("🔗 Request URL: {}", apiUrl);

                // HTTP GET 요청 설정
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                // 응답 코드 확인
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    log.error("❌ API 호출 실패: HTTP {}", responseCode);
                    conn.disconnect();
                    break;
                }

                // 응답 데이터 읽기
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                conn.disconnect();

                // JSON 파싱 (ObjectMapper 사용)
                JsonNode root = mapper.readTree(sb.toString());
                JsonNode dsListNode = null;

                // 응답 트리에서 "dsList" 노드를 탐색
                for (JsonNode node : root) {
                    if (node.has("dsList")) {
                        dsListNode = node.path("dsList");
                        break;
                    }
                }

                // 데이터가 없으면 종료
                if (dsListNode == null || dsListNode.isEmpty()) {
                    log.info("📭 더 이상 데이터 없음 (page={})", page);
                    break;
                }

                // 공고 리스트 순회 후 저장
                for (JsonNode obj : dsListNode) {
                    String panNm = obj.path("PAN_NM").asText("");
                    String panNtStDt = obj.path("PAN_NT_ST_DT").asText("");

                    // 중복 확인 (공고명 + 게시일)
                    Optional<LhNotice> existing =
                            lhNoticeRepository.findByPanNmAndPanNtStDt(panNm, panNtStDt);
                    if (existing.isPresent()) continue;

                    // ===============================
                    // 1) Products 먼저 생성
                    // ===============================
                    Products product = productsRepository.save(
                            Products.builder()
                                    .type(ProductType.HOUSING)
                                    .name(panNm)                      // 공고명
                                    .provider("LH 한국토지주택공사")      // 제공기관
                                    .detailUrl(obj.path("DTL_URL").asText(""))
                                    .build()
                    );

                    // ===============================
                    // 2) LhNotice 생성 + product 매핑
                    // ===============================
                    LhNotice notice = LhNotice.builder()
                            .product(product) // product_id 매핑
                            .uppAisTpNm(obj.path("UPP_AIS_TP_NM").asText(""))
                            .aisTpCdNm(obj.path("AIS_TP_CD_NM").asText(""))
                            .panNm(panNm)
                            .cnpCdNm(obj.path("CNP_CD_NM").asText(""))
                            .panSs(obj.path("PAN_SS").asText(""))
                            .panNtStDt(panNtStDt)
                            .clsgDt(obj.path("CLSG_DT").asText(""))
                            .dtlUrl(obj.path("DTL_URL").asText(""))
                            .build();

                    lhNoticeRepository.save(notice);
                    totalCount++;
                }

                log.info("📄 LH 공고 수집 중... 현재 페이지: {}", page);
                page++;
            }

            log.info("✅ LH 공고 데이터 수집 완료 (총 {}건 저장)", totalCount);

        } catch (Exception e) {
            log.error("❌ LH 공고 수집 실패: {}", e.getMessage(), e);
        }
    }

    /** 프로젝트 전체 구조 통일용 Wrapper 메서드 */
    public void syncNotices() {
        fetchNotices();
    }
}
