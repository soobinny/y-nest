package com.example.capstonedesign.application.ingest.Youth;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * YouthPolicyScheduler 단위 테스트
 * - 스케줄러 메서드가 YouthPolicyIngestService에 작업을 위임하는지 검증
 * - @Scheduled 설정(cron, zone)을 리플렉션으로 검증
 */
@ExtendWith(MockitoExtension.class)
class YouthPolicySchedulerTest {

    @Mock
    private YouthPolicyIngestService ingestService;

    @InjectMocks
    private YouthPolicyScheduler youthPolicyScheduler;

    @Test
    @DisplayName("syncYouthPolicies()가 청년정책 전체 수집을 서비스에 위임한다")
    void syncYouthPolicies_callsIngestAllPolicies() {
        // when
        youthPolicyScheduler.syncYouthPolicies();

        // then
        verify(ingestService, times(1)).ingestAllPolicies();
    }

    @Test
    @DisplayName("@Scheduled 설정이 06시/18시 Asia/Seoul로 등록되어 있다")
    void scheduledAnnotationConfiguration() throws NoSuchMethodException {
        // given
        Method method = YouthPolicyScheduler.class.getDeclaredMethod("syncYouthPolicies");

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
