package com.example.capstonedesign.application.ingest.SH;

import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShIngestController.class)
@AutoConfigureMockMvc
class ShIngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShIngestService shIngestService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @WithMockUser(username = "admin", roles = "ADMIN") // Security 설정에 맞게 조정
    @Test
    void sync_callsCrawlAll_andReturnsOk() throws Exception {
        // when & then
        mockMvc.perform(post("/admin/sh/sync")
                        .with(csrf()))
                .andExpect(status().isOk());

        // ShIngestService.crawlAll()이 한 번 호출되었는지 검증
        verify(shIngestService).crawlAll();
    }
}
