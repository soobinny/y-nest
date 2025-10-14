package com.example.capstonedesign.infra.email;

import com.example.capstonedesign.domain.users.port.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ConsoleEmailSender
 * -------------------------------------------------
 * - {@link EmailSender} 인터페이스의 단순 구현체
 * - 실제 메일 전송 대신 콘솔 로그로 이메일 내용을 출력
 * - 개발 및 테스트 환경에서 사용
 */
@Slf4j
@Component
public class ConsoleEmailSender implements EmailSender {

    /**
     * 이메일 전송 시뮬레이션
     * - 메일 내용을 콘솔 로그(INFO 레벨)로 출력
     *
     * @param to      수신자 이메일 주소
     * @param subject 메일 제목
     * @param content 메일 본문 내용
     */
    @Override
    public void send(String to, String subject, String content) {
        log.info("[MAIL] to={}, subject={}, content={}", to, subject, content);
    }
}
