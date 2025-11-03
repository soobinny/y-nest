package com.example.capstonedesign.application.ingest.Finance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinlifeIngestScheduler {

    private final FinlifeIngestService service;

    @Value("${finlife.ingest.cron}")
    private String cron; // 문서화용(실제 사용은 @Scheduled)

    /** 실제 운영 시: 06시 / 18시 실행
     *  cron 예시 → "0 0 6,18 * * *"
     */
    @Scheduled(cron = "0 0 6,18 * * *", zone = "Asia/Seoul")
    @Scheduled(cron = "${finlife.ingest.cron}")
    public void runNightly() {
        log.info("FinLife nightly ingest start");
        int company = service.syncCompanies(10);
        int products = service.syncDepositAndSaving(20);
        int loans = service.syncLoans(20);
        log.info("FinLife nightly ingest done - companies: {}, products: {}, loans: {}", company, products, loans);
    }
}
