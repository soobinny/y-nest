package com.example.capstonedesign.domain.users.controller;

import com.example.capstonedesign.domain.users.dto.request.PasswordResetConfirmRequest;
import com.example.capstonedesign.domain.users.dto.request.PasswordResetRequest;
import com.example.capstonedesign.domain.users.dto.request.SignupRequest;
import com.example.capstonedesign.domain.users.dto.response.UsersResponse;
import com.example.capstonedesign.domain.users.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * UsersController
 * -------------------------------------------------
 * 회원 관련 API
 * - 회원 가입
 * - 본인확인 (아이디 찾기)
 * - 비밀번호 재설정
 * -------------------------------------------------
 * ※ 모든 이메일 발송은 SMTP 기반으로 처리됨
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "회원 관련 API (회원 가입, 본인 확인, 아이디 찾기, 비밀번호 재설정)")
public class UsersController {

    private final UsersService usersService;

    // ---------------------------------------------------------
    // 1. 회원 가입
    // ---------------------------------------------------------
    @Operation(summary = "회원 가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가입 성공",
                    content = @Content(schema = @Schema(implementation = UsersResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<UsersResponse> signup(@Valid @RequestBody SignupRequest req) {
        return ResponseEntity.ok(usersService.signup(req));
    }

    // ---------------------------------------------------------
    // 2. 아이디(이메일) 찾기 - 인증 번호 발송
    // ---------------------------------------------------------
    @Operation(summary = "아이디(이메일) 찾기 - 인증 번호 발송",
            description = "이름과 이메일이 일치하는 사용자가 존재하면 인증 번호를 이메일로 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 번호 발송 성공"),
            @ApiResponse(responseCode = "404", description = "일치하는 사용자 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/find-id/request")
    public ResponseEntity<String> requestIdVerification(
            @RequestParam String name,
            @RequestParam String email
    ) {
        usersService.sendIdVerificationCode(name, email);
        return ResponseEntity.ok("인증 번호가 이메일로 발송되었습니다.");
    }

    // ---------------------------------------------------------
    // 3. 아이디(이메일) 찾기 - 인증 번호 확인
    // ---------------------------------------------------------
    @Operation(summary = "아이디(이메일) 찾기 - 인증 번호 확인",
            description = "입력한 인증 번호가 일치하면 본인 확인된 이메일(아이디)을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 성공 및 이메일 반환"),
            @ApiResponse(responseCode = "401", description = "인증 번호 불일치 또는 만료",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/find-id/confirm")
    public ResponseEntity<String> confirmIdVerification(
            @RequestParam String email,
            @RequestParam String code
    ) {
        String verifiedEmail = usersService.confirmIdVerification(email, code);
        return ResponseEntity.ok("회원님의 아이디(이메일)는 " + verifiedEmail + " 입니다.");
    }

    // ---------------------------------------------------------
    // 4. 비밀번호 재설정 요청 (메일 전송)
    // ---------------------------------------------------------
    @Operation(summary = "비밀번호 재설정 요청", description = "이메일로 비밀번호 재설정 링크를 발송합니다.")
    @ApiResponse(responseCode = "200", description = "비밀번호 재설정 메일 발송")
    @PostMapping("/password-reset/request")
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody PasswordResetRequest req) {
        usersService.requestPasswordReset(req.email());
        return ResponseEntity.ok("비밀번호 재설정 안내 메일이 발송되었습니다.");
    }

    // ---------------------------------------------------------
    // 5. 비밀번호 재설정 확정
    // ---------------------------------------------------------
    @Operation(summary = "비밀번호 재설정 확정", description = "토큰 검증 후 새 비밀번호로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 만료 또는 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<String> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        usersService.confirmPasswordReset(req.token(), req.newPassword());
        return ResponseEntity.ok("비밀번호가 성공적으로 재설정되었습니다.");
    }

    // ---------------------------------------------------------
    // 공통 에러 응답 포맷 (Swagger 문서 표시용)
    // ---------------------------------------------------------
    @Schema(name = "ApiError", description = "에러 응답 포맷")
    static class ApiError {
        public String code;
        public String message;
        public ApiError() {}
        public ApiError(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}

