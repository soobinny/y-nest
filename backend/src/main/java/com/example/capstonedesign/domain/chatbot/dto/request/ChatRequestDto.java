package com.example.capstonedesign.domain.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequestDto {

    @NotBlank(message = "message는 비어 있을 수 없습니다.")
    private String message;
}

