package com.example.capstonedesign.infra.email;

import com.example.capstonedesign.domain.users.port.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * SMTP 이메일 전송기
 * -------------------------
 * - JavaMailSender 기반 메일 발송 구현체
 * - @Async 비동기 처리로 API 응답 지연 방지
 * - HTML 본문 자동 감지 및 전송
 * - 예외 발생 시 상세 로그 출력
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    /**
     * 이메일 전송
     *
     * @param to 수신자 주소
     * @param subject 제목
     * @param body 본문 (HTML 또는 텍스트)
     */
    @Override
    @Async
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, body.contains("<html>"));

            mailSender.send(message);
            log.info("✅ 메일 전송 성공 → {}", to);

        } catch (MessagingException e) {
            log.error("❌ 메일 구성 실패 → {}, {}", to, e.getMessage(), e);
            throw new RuntimeException("메일 메시지 생성 실패", e);
        } catch (MailException e) {
            log.error("❌ 메일 전송 실패 → {}, {}", to, e.getMessage(), e);
            throw new RuntimeException("메일 전송 실패", e);
        } catch (Exception e) {
            log.error("❌ 메일 오류 → {}, {}", to, e.getMessage(), e);
            throw new RuntimeException("메일 전송 중 오류 발생", e);
        }
    }
}