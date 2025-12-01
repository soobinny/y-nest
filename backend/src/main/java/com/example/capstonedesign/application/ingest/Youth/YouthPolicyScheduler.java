package com.example.capstonedesign.application.ingest.Youth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class YouthPolicyScheduler {

    private final YouthPolicyIngestService ingestService;

    /** 실제 운영 시: 06시 / 18시 실행
     *  cron 예시 → "0 0 6,18 * * *"
     */
    @Scheduled(cron = "0 0 6,18 * * *", zone = "Asia/Seoul")
    public void syncYouthPolicies() {
        log.info("🚀 청년정책 데이터 수집 시작");
        ingestService.ingestAllPolicies();
        log.info("✅ 청년정책 데이터 수집 완료");
    }
}
