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

    /** 30분마다 실행 (테스트용)
     *  실제 운영 시: 06시 / 18시 실행
     *  cron 예시 → "0 0 6,18 * * *"
     */
    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Seoul") // 1분마다 0초에 실행
    // 매일 새벽 3시에 정책 갱신
    // @Scheduled(cron = "0 0 3 * * *")
    public void syncYouthPolicies() {
        log.info("🚀 청년정책 데이터 수집 시작");
        ingestService.ingestAllPolicies();
        log.info("✅ 청년정책 데이터 수집 완료");
    }
}
