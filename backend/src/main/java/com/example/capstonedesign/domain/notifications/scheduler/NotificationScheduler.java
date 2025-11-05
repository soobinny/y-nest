package com.example.capstonedesign.domain.notifications.scheduler;

import com.example.capstonedesign.domain.notifications.service.NotificationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * NotificationScheduler
 * -------------------------------------------------------
 * 매일 정해진 시각에 통합 알림(주거/금융/정책) 이메일 발송 트리거
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationsService service;

    /** 매일 오전 6시 실행 */
//    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")  // 테스트용 (1분마다)
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    public void run() {
        log.info("🕕 마감 임박 공고 알림 스케줄러 실행");
        service.sendDailyDigest();
    }
}
