package com.example.capstonedesign.domain.users.port;

/**
 * EmailSender
 * -------------------------------------------------
 * - 이메일 전송을 위한 포트 인터페이스
 * - 구현체는 실제 메일 서비스(SMTP, AWS SES, Gmail API 등)에 따라 달라짐
 */
public interface EmailSender {

    /**
     * 이메일 전송
     *
     * @param to      수신자 이메일 주소
     * @param subject 메일 제목
     * @param content 메일 본문 내용
     */
    void send(String to, String subject, String content);
}
