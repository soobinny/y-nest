package com.example.capstonedesign.application.ingest;

import com.example.capstonedesign.application.ingest.Finance.FinlifeIngestService;
import com.example.capstonedesign.application.ingest.LH.LhLeaseNoticeService;
import com.example.capstonedesign.application.ingest.SH.ShIngestService;
import com.example.capstonedesign.application.ingest.Youth.YouthPolicyIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    // 서비스별 데이터 수집 서비스 주입
    private final FinlifeIngestService finlifeIngestService;         // 금융상품
    private final LhLeaseNoticeService lhIngestService;              // LH 주거공고
    private final ShIngestService shIngestService;                   // SH 주거공고
    private final YouthPolicyIngestService youthPolicyIngestService; // 청년정책

    /**
     * 애플리케이션 실행 후 외부 API 데이터를 초기 동기화하는 메서드
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initData() {
        log.info("🔹 [INIT] 서버 시작 - 초기 데이터 동기화 시작");

        try {
            log.info("🏦 금융상품 동기화 시작");
            finlifeIngestService.syncCompanies(3);
            finlifeIngestService.syncDepositAndSaving(3);
            finlifeIngestService.syncLoans(3);
            log.info("✅ 금융상품 동기화 완료");
        } catch (Exception e) {
            log.error("❌ 금융 동기화 실패", e);
        }

        try {
            log.info("🏠 LH 주거공고 동기화 시작");
            lhIngestService.syncNotices();
            log.info("✅ LH 동기화 완료");
        } catch (Exception e) {
            log.error("❌ LH 동기화 실패", e);
        }

        try {
            log.info("🏢 SH 주거공고 동기화 시작");
            shIngestService.syncNotices();
            log.info("✅ SH 동기화 완료");
        } catch (Exception e) {
            log.error("❌ SH 동기화 실패", e);
        }

        try {
            log.info("💡 청년정책 동기화 시작");
            youthPolicyIngestService.syncPolicies();
            log.info("✅ 청년정책 동기화 완료");
        } catch (Exception e) {
            log.error("❌ 청년정책 동기화 실패", e);
        }

        log.info("🎉 [INIT COMPLETE] 모든 초기 데이터 동기화 완료!");
    }
}

