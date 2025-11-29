package com.example.capstonedesign.application.ingest.Finance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinlifeIngestSchedulerTest {

    @Mock
    private FinlifeIngestService finlifeIngestService;

    @InjectMocks
    private FinlifeIngestScheduler scheduler;

    @Test
    @DisplayName("runNightly() 호출 시 회사/수신/대출 동기화가 순서대로 한 번씩 호출된다")
    void runNightly_callsAllSyncMethods() {
        // given
        when(finlifeIngestService.syncCompanies(10)).thenReturn(3);
        when(finlifeIngestService.syncDepositAndSaving(20)).thenReturn(5);
        when(finlifeIngestService.syncLoans(20)).thenReturn(7);

        // when
        scheduler.runNightly();

        // then - 각 메서드가 한 번씩, 올바른 파라미터로 호출되었는지 검증
        verify(finlifeIngestService, times(1)).syncCompanies(10);
        verify(finlifeIngestService, times(1)).syncDepositAndSaving(20);
        verify(finlifeIngestService, times(1)).syncLoans(20);

        // (선택) 호출 순서까지 검증하고 싶다면
        InOrder inOrder = inOrder(finlifeIngestService);
        inOrder.verify(finlifeIngestService).syncCompanies(10);
        inOrder.verify(finlifeIngestService).syncDepositAndSaving(20);
        inOrder.verify(finlifeIngestService).syncLoans(20);

        // 추가로 불필요한 호출이 없는지도 체크 (선택)
        verifyNoMoreInteractions(finlifeIngestService);
    }
}
