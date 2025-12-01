package com.example.capstonedesign.infra.email;

import com.example.capstonedesign.domain.users.port.EmailSender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SMTP 메일 전송 테스트용 컨트롤러
 * -------------------------------------
 * - Swagger에서 직접 메일 발송 기능 확인 가능
 * - 실제 서비스 로직과는 무관한 개발/테스트용 엔드포인트
 */
@RestController
@RequestMapping("/test/email")
@RequiredArgsConstructor
@Tag(name = "Email Test", description = "SMTP 메일 전송 테스트 API")
public class EmailTestController {

    private final EmailSender emailSender;

    /**
     * 테스트 메일 전송
     * - SMTP 설정 및 연결 상태를 확인하기 위한 엔드포인트
     *
     * @param to 수신자 이메일
     * @param subject 제목 (기본값: [Y-Nest] SMTP 메일 테스트)
     * @param body 본문 (기본값: 테스트 메시지)
     */
    @Operation(
            summary = "테스트 메일 전송",
            description = "SMTP 설정이 정상인지 확인하기 위한 테스트용 메일 발송 API입니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "메일 전송 성공",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "500", description = "메일 전송 실패",
                            content = @Content(schema = @Schema(implementation = String.class)))
            }
    )
    @PostMapping
    public ResponseEntity<String> sendTestMail(
            @RequestParam String to,
            @RequestParam(required = false, defaultValue = "[Y-Nest] SMTP 메일 테스트") String subject,
            @RequestParam(required = false, defaultValue = "이 메일은 SMTP 설정 테스트용입니다.") String body
    ) {
        emailSender.send(to, subject, body);
        return ResponseEntity.ok("✅ 테스트 메일이 전송되었습니다 → 수신자: " + to);
    }
}
