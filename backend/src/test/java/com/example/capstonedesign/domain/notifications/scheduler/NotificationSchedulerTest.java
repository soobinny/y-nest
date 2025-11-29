package com.example.capstonedesign.domain.notifications.scheduler;

import com.example.capstonedesign.domain.notifications.service.NotificationsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * NotificationScheduler 단위 테스트
 * ---------------------------------------------
 * - run()이 호출되면 NotificationsService.sendDailyDigest()를 위임 호출하는지 검증
 * - @Scheduled 애노테이션의 cron, zone 설정이 올바른지 리플렉션으로 검증
 */
@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock
    private NotificationsService notificationsService;

    @InjectMocks
    private NotificationScheduler notificationScheduler;

    @Test
    @DisplayName("run() 호출 시 NotificationsService.sendDailyDigest()를 한 번 호출한다")
    void run_callsSendDailyDigestOnce() {
        // when
        notificationScheduler.run();

        // then
        verify(notificationsService, times(1)).sendDailyDigest();
    }

    @Test
    @DisplayName("@Scheduled 애노테이션이 존재하고 cron과 time zone이 올바르게 설정되어 있다")
    void scheduledAnnotation_hasCorrectCronAndZone() throws NoSuchMethodException {
        // given
        Method runMethod = NotificationScheduler.class.getMethod("run");

        // when
        Scheduled scheduled = runMethod.getAnnotation(Scheduled.class);

        // then
        assertThat(scheduled).as("@Scheduled 애노테이션이 있어야 함").isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 0 9 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }
}
