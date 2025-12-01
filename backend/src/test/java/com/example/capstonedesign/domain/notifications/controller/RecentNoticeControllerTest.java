package com.example.capstonedesign.domain.notifications.controller;

import com.example.capstonedesign.domain.notifications.dto.RecentNoticeDto;
import com.example.capstonedesign.domain.notifications.service.RecentNoticeService;
import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = RecentNoticeController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecentNoticeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RecentNoticeService recentNoticeService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /api/notices/recent - 최근 게시물 Map 구조 정상 반환")
    void getRecentNotices_returnsMapFromService() throws Exception {
        // given
        RecentNoticeDto mockDto1 = Mockito.mock(RecentNoticeDto.class);
        RecentNoticeDto mockDto2 = Mockito.mock(RecentNoticeDto.class);

        Map<String, List<RecentNoticeDto>> stub = Map.of(
                "all", List.of(mockDto1),
                "housing", List.of(mockDto1),
                "policy", List.of(mockDto2)
        );

        when(recentNoticeService.getRecentNotices()).thenReturn(stub);

        // when & then
        mockMvc.perform(get("/api/notices/recent"))
                .andExpect(status().isOk())
                // 응답 최상단에 키 존재 여부
                .andExpect(jsonPath("$.all").isArray())
                .andExpect(jsonPath("$.housing").isArray())
                .andExpect(jsonPath("$.policy").isArray())
        ;

        // 서비스 메서드가 정확히 한 번 호출됐는지 검증
        verify(recentNoticeService, times(1)).getRecentNotices();
        verifyNoMoreInteractions(recentNoticeService);
    }
}
