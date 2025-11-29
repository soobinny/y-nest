package com.example.capstonedesign.application.ingest.LH;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * LhScheduler 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class LhSchedulerTest {

    @Mock
    LhHousingIngestService lhService;          // 현재는 사용 안 하지만 주입됨

    @Mock
    LhLeaseNoticeService lhLeaseNoticeService;

    @InjectMocks
    LhScheduler lhScheduler;

    @Test
    @DisplayName("fetchLeaseNotices()가 LH 임대·분양 공고 수집 서비스를 호출한다")
    void fetchLeaseNotices_callsLeaseNoticeService() {
        // given
        doNothing().when(lhLeaseNoticeService).fetchNotices();

        // when
        lhScheduler.fetchLeaseNotices();

        // then
        verify(lhLeaseNoticeService, times(1)).fetchNotices();
        // lhService는 이 메서드에서는 사용되지 않아야 함
        verifyNoInteractions(lhService);
    }

    @Test
    @DisplayName("@Scheduled 설정이 06시/18시 Asia/Seoul로 등록되어 있다")
    void scheduledAnnotationConfiguration() throws NoSuchMethodException {
        // given
        Method method = LhScheduler.class.getDeclaredMethod("fetchLeaseNotices");

        // when
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        // then
        assertThat(scheduled)
                .as("@Scheduled 애노테이션이 존재해야 한다")
                .isNotNull();

        assertEquals("0 0 6,18 * * *", scheduled.cron(),
                "cron 표현식이 06시/18시 두 번 실행으로 설정되어야 한다");
        assertEquals("Asia/Seoul", scheduled.zone(),
                "Time zone이 Asia/Seoul로 설정되어야 한다");
    }
}
