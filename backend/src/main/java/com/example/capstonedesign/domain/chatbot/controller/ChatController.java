package com.example.capstonedesign.domain.chatbot.controller;

import com.example.capstonedesign.domain.chatbot.dto.request.ChatRequestDto;
import com.example.capstonedesign.domain.chatbot.dto.response.ChatResponseDto;
import com.example.capstonedesign.domain.chatbot.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat", description = "DB 기반 Y-Nest 검색 챗봇 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "챗봇과 대화 (DB 기반)",
            description = "사용자 메시지를 전달하면, Y-Nest DB를 검색해서 간단한 안내를 반환합니다."
    )
    @PostMapping
    public ResponseEntity<ChatResponseDto> chat(@Valid @RequestBody ChatRequestDto requestDto) {
        ChatResponseDto responseDto = chatService.chat(requestDto);
        return ResponseEntity.ok(responseDto);
    }
}
