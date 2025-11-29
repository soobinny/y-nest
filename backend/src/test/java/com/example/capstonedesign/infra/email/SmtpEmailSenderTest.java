package com.example.capstonedesign.infra.email;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * SmtpEmailSender 단위 테스트
 */
class SmtpEmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private SmtpEmailSender smtpEmailSender;

    @Captor
    private ArgumentCaptor<MimeMessage> mimeCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -------------------------------
    // send(String to, String subject, String body)
    // -------------------------------
    // -------------------------------
// send(String to, String subject, String body)
// -------------------------------
    @Test
    @DisplayName("send - 일반 텍스트 메일 전송 성공")
    void send_plainText_success() throws Exception {
        // given
        String to = "user@example.com";
        String subject = "테스트 메일";
        String body = "본문입니다.";

        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        smtpEmailSender.send(to, subject, body);

        // then
        verify(mailSender).send(mimeCaptor.capture());
        MimeMessage sent = mimeCaptor.getValue();

        // 수신자 / 제목 검증
        assertThat(((InternetAddress) sent.getRecipients(Message.RecipientType.TO)[0]).getAddress())
                .isEqualTo(to);
        assertThat(sent.getSubject()).isEqualTo(subject);

        // 본문(content) 검증 - String 또는 MimeMultipart 모두 대응
        Object content = sent.getContent();
        String text;

        if (content instanceof String s) {
            // 단일 텍스트 메일인 경우
            text = s;
        } else if (content instanceof MimeMultipart multipart) {
            // 멀티파트 메일인 경우 → 모든 파트 중 String 본문을 모아서 하나의 문자열로 만든다.
            StringBuilder sb = new StringBuilder();
            int count = multipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart part = multipart.getBodyPart(i);
                Object partContent = part.getContent();

                if (partContent instanceof String s) {
                    sb.append(s);
                } else if (partContent instanceof MimeMultipart nested) {
                    // nested multipart까지 방어적으로 처리
                    int nestedCount = nested.getCount();
                    for (int j = 0; j < nestedCount; j++) {
                        BodyPart nestedPart = nested.getBodyPart(j);
                        Object nestedContent = nestedPart.getContent();
                        if (nestedContent instanceof String ns) {
                            sb.append(ns);
                        }
                    }
                }
            }
            text = sb.toString();
        } else {
            throw new IllegalStateException("예상치 못한 메일 본문 타입: " + content.getClass());
        }

        assertThat(text).isNotBlank();
        assertThat(text).isEqualTo(body);
    }

    @Test
    @DisplayName("send - <html> 포함 본문(HTML) 전송 성공")
    void send_htmlBody_success() throws Exception {
        // given
        String to = "user@example.com";
        String subject = "HTML 메일";
        String body = "<html><body><h1>안녕하세요</h1></body></html>";

        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        smtpEmailSender.send(to, subject, body);

        // then
        verify(mailSender).send(mimeCaptor.capture());
        MimeMessage sent = mimeCaptor.getValue();

        assertThat(((InternetAddress) sent.getRecipients(Message.RecipientType.TO)[0]).getAddress())
                .isEqualTo(to);
        assertThat(sent.getSubject()).isEqualTo(subject);

        // HTML 모드로 들어간 경우, Multipart로 감싸질 수 있음 → 타입만 간단히 확인
        Object content = sent.getContent();
        // String 이거나 Multipart 둘 다 가능하니 null 아님만 체크
        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("send - MailException 발생 시 RuntimeException으로 래핑")
    void send_mailException_throwsRuntime() {
        // given
        String to = "user@example.com";
        String subject = "에러 메일";
        String body = "본문";

        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // mailSender.send() 호출 시 MailException 던지도록 설정
        doThrow(new MailSendException("SMTP 서버 오류"))
                .when(mailSender).send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() -> smtpEmailSender.send(to, subject, body))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("메일 전송 실패");
    }

    // -------------------------------
    // sendHtml(String to, String subject, String htmlBody)
    // -------------------------------

    @Test
    @DisplayName("sendHtml - HTML 메일 전송 성공")
    void sendHtml_success() throws Exception {
        // given
        String to = "user@example.com";
        String subject = "HTML 전용 메일";
        String html = "<html><body><p>HTML 바디</p></body></html>";

        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        smtpEmailSender.sendHtml(to, subject, html);

        // then
        verify(mailSender).send(mimeCaptor.capture());
        MimeMessage sent = mimeCaptor.getValue();

        assertThat(((InternetAddress) sent.getRecipients(Message.RecipientType.TO)[0]).getAddress())
                .isEqualTo(to);
        assertThat(sent.getSubject()).isEqualTo(subject);

        Object content = sent.getContent();

        assertThat(content).isNotNull();
    }

    @Test
    @DisplayName("sendHtml - 예외 발생 시 RuntimeException으로 래핑")
    void sendHtml_genericException() {
        // given
        String to = "user@example.com";
        String subject = "에러 HTML";
        String html = "<html>에러</html>";

        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // mailSender.send() 호출 시 RuntimeException 발생
        doThrow(new RuntimeException("SMTP 연결 실패"))
                .when(mailSender).send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() -> smtpEmailSender.sendHtml(to, subject, html))
                .isInstanceOf(RuntimeException.class)
                .satisfies(ex ->
                        assertThat(ex.getCause()).isInstanceOf(RuntimeException.class)
                );
    }

    @Test
    @DisplayName("send - 알 수 없는 예외 발생 시 '메일 전송 중 오류 발생'으로 래핑")
    void send_genericException_throwsRuntime() {
        // given
        String to = "user@example.com";
        String subject = "에러 메일";
        String body = "본문";

        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // MailException이 아닌 일반 RuntimeException 던지게 설정
        doThrow(new RuntimeException("예상치 못한 오류"))
                .when(mailSender).send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() -> smtpEmailSender.send(to, subject, body))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("메일 전송 중 오류 발생");
    }
}
