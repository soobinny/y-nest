package com.example.capstonedesign.application.ingest;

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

    // 매일 03:10 (기본값은 properties에서 관리)
    @Scheduled(cron = "${finlife.ingest.cron}")
    public void runNightly() {
        log.info("FinLife nightly ingest start");
        int company = service.syncCompanies(10);
        int products = service.syncDepositAndSaving(20);
        log.info("FinLife nightly ingest done - companies: {}, products: {}", company, products);
    }
}
