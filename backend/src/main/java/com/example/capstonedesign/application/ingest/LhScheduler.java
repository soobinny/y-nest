package com.example.capstonedesign.application.ingest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * LhScheduler
 * -----------------------------------------------------
 * - LH(한국토지주택공사) 공고 데이터를 주기적으로 수집하기 위한 스케줄러 컴포넌트
 * - Spring의 @Scheduled 기능을 사용해 일정 주기로 LhHousingIngestService를 실행함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LhScheduler {

    private final LhHousingIngestService lhService;

    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Seoul") // 30분마다 0초에 실행
    // 실제 운영 시에는 하루 두 번 새 공고 수집
    // @Scheduled(cron = "0 0 6,18 * * *", zone = "Asia/Seoul")
    public void run() {
        log.info("🏠 LH 공고 스케줄러 실행");
        lhService.ingest();
    }
}