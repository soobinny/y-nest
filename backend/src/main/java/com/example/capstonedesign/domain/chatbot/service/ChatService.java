package com.example.capstonedesign.domain.chatbot.service;

import com.example.capstonedesign.domain.chatbot.dto.request.ChatRequestDto;
import com.example.capstonedesign.domain.chatbot.dto.response.ChatResponseDto;

public interface ChatService {

    ChatResponseDto chat(ChatRequestDto requestDto);
}

