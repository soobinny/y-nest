package com.example.capstonedesign.application.ingest.LH;

import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LhIngestController - MVC 슬라이스 테스트
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(LhIngestController.class)
class LhIngestControllerMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LhHousingIngestService service;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /admin/ingest/lh/all 호출 시 서비스가 실행되고 200 OK와 완료 메시지를 반환한다")
    void ingestAll_endpointWorksWithAdminAndCsrf() throws Exception {
        // given
        doNothing().when(service).ingest();

        // when & then
        mockMvc.perform(
                        post("/admin/ingest/lh/all")
                                .with(csrf())                              // CSRF 토큰 추가
                                .with(user("admin").roles("ADMIN"))        // 인증 + ROLE_ADMIN
                )
                .andExpect(status().isOk())
                .andExpect(content().string("LH 공고 수집 완료"));

        // 서비스가 정확히 한 번 호출되었는지 검증
        verify(service, times(1)).ingest();
    }
}
