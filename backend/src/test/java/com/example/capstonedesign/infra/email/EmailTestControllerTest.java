package com.example.capstonedesign.infra.email;

import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import com.example.capstonedesign.domain.users.port.EmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailTestController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmailTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailSender emailSender;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    void sendTestMail_success_withDefaultSubjectAndBody() throws Exception {
        // given
        String to = "test@example.com";

        // when & then
        mockMvc.perform(post("/test/email")
                        .with(csrf()) // Security 사용 중이면 필요
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("✅ 테스트 메일이 전송되었습니다")))
                .andExpect(content().string(containsString("수신자: " + to)));

        // EmailSender.send(to, subject, body)가 기본값으로 호출되었는지 검증
        verify(emailSender).send(
                eq(to),
                eq("[Y-Nest] SMTP 메일 테스트"),
                eq("이 메일은 SMTP 설정 테스트용입니다.")
        );
    }
}
