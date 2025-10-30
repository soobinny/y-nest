package com.example.capstonedesign.application.ingest.SH;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ShIngestScheduler
 * - SH공사 공고 크롤러 주기적 실행 스케줄러
 * - 현재는 테스트용(10분 간격), 운영 시 하루 2회(06시/18시) 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShIngestScheduler {

    private final ShIngestService shIngestService;

    /** 10분마다 실행 (테스트용)
     *  실제 운영 시: 06시 / 18시 실행
     *  cron 예시 → "0 0 6,18 * * *"
     */
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
//    @Scheduled(cron = "0 0 6,18 * * *", zone = "Asia/Seoul")
    public void runShCrawler() {
        log.info("🕒 SH 공사 통합 공고 크롤링 시작");
        shIngestService.crawlAll();
    }
}
