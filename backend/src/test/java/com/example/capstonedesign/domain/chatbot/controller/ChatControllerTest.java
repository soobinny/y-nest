package com.example.capstonedesign.domain.chatbot.controller;

import com.example.capstonedesign.domain.chatbot.dto.request.ChatRequestDto;
import com.example.capstonedesign.domain.chatbot.dto.response.ChatResponseDto;
import com.example.capstonedesign.domain.chatbot.service.ChatService;
import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    void chatEndpoint_returnsReplyFromService() throws Exception {
        // given
        ChatRequestDto requestDto = new ChatRequestDto();
        requestDto.setMessage("서울 전세 뭐 있어?");

        ChatResponseDto responseDto = ChatResponseDto.builder()
                .reply("테스트 응답입니다.")
                .build();

        when(chatService.chat(any(ChatRequestDto.class)))
                .thenReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("테스트 응답입니다."));

        verify(chatService).chat(any(ChatRequestDto.class));
    }
}
