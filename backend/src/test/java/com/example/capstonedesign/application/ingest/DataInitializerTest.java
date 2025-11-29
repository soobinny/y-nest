package com.example.capstonedesign.application.ingest;

import com.example.capstonedesign.application.ingest.Finance.FinlifeIngestService;
import com.example.capstonedesign.application.ingest.LH.LhLeaseNoticeService;
import com.example.capstonedesign.application.ingest.SH.ShIngestService;
import com.example.capstonedesign.application.ingest.Youth.YouthPolicyIngestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * DataInitializer 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    FinlifeIngestService finlifeIngestService;

    @Mock
    LhLeaseNoticeService lhIngestService;

    @Mock
    ShIngestService shIngestService;

    @Mock
    YouthPolicyIngestService youthPolicyIngestService;

    @InjectMocks
    DataInitializer dataInitializer;

    // ---------------------------------------------------------------------
    // 1. 초기 데이터가 이미 있을 때 → init 스킵
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("initData() - 초기 데이터가 있으면 이후 동기화를 모두 스킵한다")
    void initData_skipsWhenInitialDataExists() {
        // given
        when(finlifeIngestService.hasInitialData()).thenReturn(true);

        // when
        dataInitializer.initData();

        // then
        verify(finlifeIngestService, times(1)).hasInitialData();
        verify(finlifeIngestService, never()).syncCompanies(anyInt());
        verify(finlifeIngestService, never()).syncDepositAndSaving(anyInt());
        verify(finlifeIngestService, never()).syncLoans(anyInt());

        verify(lhIngestService, never()).syncNotices();
        verify(shIngestService, never()).syncNotices();
        verify(youthPolicyIngestService, never()).syncPolicies();
    }

    // ---------------------------------------------------------------------
    // 2. 정상 플로우 - hasInitialData=false → 모든 동기화 호출
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("initData() - 초기 데이터가 없으면 금융/LH/SH/청년정책 동기화를 순차적으로 수행한다")
    void initData_runsAllSyncWhenNoInitialData() {
        // given
        when(finlifeIngestService.hasInitialData()).thenReturn(false);

        // when
        dataInitializer.initData();

        // then
        verify(finlifeIngestService, times(1)).hasInitialData();
        verify(finlifeIngestService, times(1)).syncCompanies(3);
        verify(finlifeIngestService, times(1)).syncDepositAndSaving(3);
        verify(finlifeIngestService, times(1)).syncLoans(3);

        verify(lhIngestService, times(1)).syncNotices();
        verify(shIngestService, times(1)).syncNotices();
        verify(youthPolicyIngestService, times(1)).syncPolicies();
    }

    // ---------------------------------------------------------------------
    // 3. 예외 발생 플로우 - 각 try/catch 블록이 예외를 삼키고 다음으로 진행하는지
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("initData() - 각 동기화 단계에서 예외가 발생해도 전체 초기화는 중단되지 않는다")
    void initData_handlesExceptionsPerSection() {
        // given
        when(finlifeIngestService.hasInitialData()).thenReturn(false);

        // 금융: 첫 메서드에서 예외
        doThrow(new RuntimeException("금융 동기화 실패"))
                .when(finlifeIngestService).syncCompanies(3);

        // LH / SH / 청년정책도 예외 던지게 설정
        doThrow(new RuntimeException("LH 동기화 실패"))
                .when(lhIngestService).syncNotices();
        doThrow(new RuntimeException("SH 동기화 실패"))
                .when(shIngestService).syncNotices();
        doThrow(new RuntimeException("청년정책 동기화 실패"))
                .when(youthPolicyIngestService).syncPolicies();

        // when & then
        // 예외가 밖으로 전파되지 않고 메서드가 끝까지 실행되어야 한다.
        assertDoesNotThrow(() -> dataInitializer.initData());

        verify(finlifeIngestService, times(1)).syncCompanies(3);
        // syncDepositAndSaving, syncLoans는 syncCompanies에서 예외가 나면 호출되지 않을 수 있음
        verify(lhIngestService, times(1)).syncNotices();
        verify(shIngestService, times(1)).syncNotices();
        verify(youthPolicyIngestService, times(1)).syncPolicies();
    }

    // ---------------------------------------------------------------------
    // 4. @EventListener(ApplicationReadyEvent) 애노테이션 검증 (선택)
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("initData()는 ApplicationReadyEvent에 대한 @EventListener로 등록되어 있다")
    void initData_hasEventListenerAnnotation() throws NoSuchMethodException {
        Method method = DataInitializer.class.getDeclaredMethod("initData");

        EventListener listener = method.getAnnotation(EventListener.class);
        assertNotNull(listener, "@EventListener 애노테이션이 있어야 한다");

        boolean listensToAppReady =
                java.util.Arrays.asList(listener.value()).contains(ApplicationReadyEvent.class) ||
                        java.util.Arrays.asList(listener.classes()).contains(ApplicationReadyEvent.class);

        assertTrue(listensToAppReady, "ApplicationReadyEvent에 대해 리스닝해야 한다");
    }
}
