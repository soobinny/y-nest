package com.example.capstonedesign.domain.notifications.service;

import com.example.capstonedesign.domain.housingannouncements.repository.LhNoticeRepository;
import com.example.capstonedesign.domain.notifications.dto.RecentNoticeDto;
import com.example.capstonedesign.domain.shannouncements.repository.ShAnnouncementRepository;
import com.example.capstonedesign.domain.youthpolicies.repository.YouthPolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecentNoticeServiceTest {

    @Mock
    LhNoticeRepository lhNoticeRepository;

    @Mock
    ShAnnouncementRepository shAnnouncementRepository;

    @Mock
    YouthPolicyRepository youthPolicyRepository;

    @InjectMocks
    RecentNoticeService recentNoticeService;

    @Test
    void getRecentNotices_returnsEmptyListsWhenNoData() {
        // given
        when(lhNoticeRepository.findTop20ByOrderByPanNtStDtDesc())
                .thenReturn(Collections.emptyList());
        when(shAnnouncementRepository.findTop20ByOrderByPostDateDesc())
                .thenReturn(Collections.emptyList());
        when(youthPolicyRepository.findActiveOrderByStartDateDesc(anyString(), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        // when
        Map<String, List<RecentNoticeDto>> result = recentNoticeService.getRecentNotices();

        // then
        assertNotNull(result);
        assertTrue(result.containsKey("all"));
        assertTrue(result.containsKey("housing"));
        assertTrue(result.containsKey("policy"));

        assertTrue(result.get("all").isEmpty());
        assertTrue(result.get("housing").isEmpty());
        assertTrue(result.get("policy").isEmpty());

        verify(lhNoticeRepository, times(1)).findTop20ByOrderByPanNtStDtDesc();
        verify(shAnnouncementRepository, times(1)).findTop20ByOrderByPostDateDesc();
        verify(youthPolicyRepository, times(1))
                .findActiveOrderByStartDateDesc(anyString(), any(PageRequest.class));
    }
}
