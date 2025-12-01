package com.example.capstonedesign.application.ingest.LH;

import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LhLeaseNoticeController 단위/슬라이스 테스트
 */
class LhLeaseNoticeControllerTest {

    // ---------------------------------------------------------------------
    // 1. 슬라이스 테스트 (@WebMvcTest + MockMvc)
    // ---------------------------------------------------------------------
    @Nested
    @ExtendWith(SpringExtension.class)
    @WebMvcTest(LhLeaseNoticeController.class)
    class LhLeaseNoticeControllerMvcTest {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        LhLeaseNoticeService lhLeaseNoticeService;

        @MockitoBean
        JwtTokenProvider jwtTokenProvider;

        @Test
        @DisplayName("POST /admin/ingest/lh/lease 호출 시 서비스가 실행되고 200 OK를 반환한다")
        void ingestLeaseNotices_endpointWorks() throws Exception {
            // given
            doNothing().when(lhLeaseNoticeService).fetchNotices();

            // when & then
            mockMvc.perform(
                            post("/admin/ingest/lh/lease")
                                    .with(csrf())
                                    .with(user("admin").roles("ADMIN"))  // ← 인증 + 권한
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string("✅ LH 분양·임대 공고 수집이 완료되었습니다."));

            // 서비스가 1번 호출되었는지 검증
            verify(lhLeaseNoticeService, times(1)).fetchNotices();
        }
    }

    // ---------------------------------------------------------------------
    // 2. 완전 단위 테스트 (Controller만 분리해서 검증)
    // ---------------------------------------------------------------------
    @Nested
    @ExtendWith(MockitoExtension.class)
    class LhLeaseNoticeControllerUnitTest {

        @Mock
        LhLeaseNoticeService lhLeaseNoticeService;

        @InjectMocks
        LhLeaseNoticeController controller;

        @Test
        @DisplayName("ingestLeaseNotices() 메서드는 서비스 호출 후 고정된 메시지를 반환한다")
        void ingestLeaseNotices_callsServiceAndReturnsMessage() {
            // given
            doNothing().when(lhLeaseNoticeService).fetchNotices();

            // when
            ResponseEntity<String> response = controller.ingestLeaseNotices();

            // then
            verify(lhLeaseNoticeService, times(1)).fetchNotices();
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody())
                    .isEqualTo("✅ LH 분양·임대 공고 수집이 완료되었습니다.");
        }
    }
}
