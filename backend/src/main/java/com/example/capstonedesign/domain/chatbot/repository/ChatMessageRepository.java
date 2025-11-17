package com.example.capstonedesign.domain.chatbot.repository;

import com.example.capstonedesign.domain.chatbot.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    Page<ChatMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
