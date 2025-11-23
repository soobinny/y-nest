package com.example.capstonedesign.domain.users.controller;

import com.example.capstonedesign.common.exception.ApiException;
import com.example.capstonedesign.common.exception.ErrorCode;
import com.example.capstonedesign.domain.notifications.entity.NotificationChannel;
import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import com.example.capstonedesign.domain.users.config.PasswordEncoder;
import com.example.capstonedesign.domain.users.dto.request.*;
import com.example.capstonedesign.domain.users.dto.response.TokenResponse;
import com.example.capstonedesign.domain.users.dto.response.UsersResponse;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "회원 관련 API (회원 가입, 본인 확인, 아이디 찾기, 비밀번호 재설정)")
public class UsersController {

    private final UsersService usersService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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
    // 2. 로그인
    // ---------------------------------------------------------
    @Operation(
            summary = "로그인 (JWT 발급)",
            description = "이메일/비밀번호 검증 후 액세스 토큰(JWT)을 발급합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그인 성공",
                            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        // (1) 사용자 조회 (활성 사용자 보장)
        Users user = usersService.requireActiveByEmail(req.email());

        // (2) 비밀번호 검증
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // (3) 토큰 발급: principal = userId(Long)
        String token = jwtTokenProvider.generate(
                Long.valueOf(user.getId()),  // id가 Integer 라면 longValue()/valueOf로 변환
                user.getEmail(),
                user.getRole()
        );
        long expiresIn = jwtTokenProvider.getExpirySeconds();

        return ResponseEntity.ok(new TokenResponse(token, "Bearer", expiresIn));
    }

    // ---------------------------------------------------------
    // 3. 내 정보 조회
    // ---------------------------------------------------------
    @Operation(
            summary = "내 정보 조회",
            description = "JWT의 principal(권장: userId)로 내 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = UsersResponse.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @GetMapping("/me")
    public ResponseEntity<UsersResponse> me(@AuthenticationPrincipal Object principal) {
        Users me = resolveUserFromPrincipal(principal);
        return ResponseEntity.ok(usersService.toResponse(me));
    }

    // ---------------------------------------------------------
    // 4. 내 정보 수정
    // ---------------------------------------------------------
    @Operation(
            summary = "내 프로필 수정",
            description = "전달된 필드만 부분 업데이트합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공",
                            content = @Content(schema = @Schema(implementation = UsersResponse.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @PutMapping("/me")
    public ResponseEntity<UsersResponse> updateMe(@Valid @RequestBody UpdateUserRequest req) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Users me = resolveUserFromPrincipal(principal);
        // UsersService에 id 기반 업데이트가 없다면 email 기반 메서드 사용
        UsersResponse updated = usersService.updateProfile(me.getEmail(), req);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/notification")
    @Operation(
            summary = "알림 수신 설정 변경",
            description = "사용자가 이메일 알림을 받을지 여부를 설정합니다. "
                    + "이 설정이 해제되면 주거공고, 금리, 청년정책 등 일일 요약 메일이 발송되지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "알림 수신 설정 변경 성공",
                            content = @Content(schema = @Schema(implementation = String.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패 또는 권한 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "해당 사용자를 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    public ResponseEntity<String> updateNotificationPreference(
            @PathVariable Integer id,
            @RequestParam boolean enabled
    ) {
        usersService.updateNotificationPreference(id, enabled);
        return ResponseEntity.ok(enabled ? "알림 수신이 활성화되었습니다." : "알림 수신이 해제되었습니다.");
    }

    @PutMapping("/notification-channel")
    @Operation(
            summary = "알림 채널 변경",
            description = "사용자가 받을 공고 마감 알림 채널(이메일, 카카오톡, 문자)을 변경합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "변경 성공",
                            content = @Content(schema = @Schema(implementation = String.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    public ResponseEntity<String> updateNotificationChannel(
            @RequestParam NotificationChannel channel
    ) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Users me = resolveUserFromPrincipal(principal);

        usersService.updateNotificationChannel(me.getId(), channel);
        return ResponseEntity.ok("알림 채널이 " + channel + "(으)로 변경되었습니다.");
    }

    // ---------------------------------------------------------
    // 5. 아이디(이메일) 찾기 - 인증 번호 발송
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
    // 6. 아이디(이메일) 찾기 - 인증 번호 확인
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
    // 7. 비밀번호 재설정 요청 (메일 전송)
    // ---------------------------------------------------------
    @Operation(summary = "비밀번호 재설정 요청", description = "이메일로 비밀번호 재설정 링크를 발송합니다.")
    @ApiResponse(responseCode = "200", description = "비밀번호 재설정 메일 발송")
    @PostMapping("/password-reset/request")
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody PasswordResetRequest req) {
        usersService.requestPasswordReset(req.email());
        return ResponseEntity.ok("비밀번호 재설정 안내 메일이 발송되었습니다.");
    }

    // ---------------------------------------------------------
    // 8. 비밀번호 재설정 확정
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
    // 9. 회원 탈퇴
    // ---------------------------------------------------------
    @Operation(
            summary = "회원 탈퇴(소프트 삭제)",
            description = "비밀번호 재확인 후 deleted=true, deleted_at 기록.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "탈퇴 성공",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패/비밀번호 불일치",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @DeleteMapping("/delete")
    public ResponseEntity<String> delete(@Valid @RequestBody DeleteRequest req) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Users me = resolveUserFromPrincipal(principal);
        String msg = usersService.delete(me.getEmail(), req.password());
        return ResponseEntity.ok(msg);
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

    // principal(Long id / String email / UserDetails.username ...)을 Users 엔티티로 해석
    private Users resolveUserFromPrincipal(Object principal) {
        if (principal instanceof Long uid) {
            // Users.id가 Integer면 변환 필요
            Integer id = Math.toIntExact(uid);
            return usersService.requireActiveById(id);
        }
        if (principal instanceof String email) {
            return usersService.requireActiveByEmail(email);
        }
        if (principal instanceof UserDetails details) {
            String username = details.getUsername();
            if (username.matches("\\d+")) {
                return usersService.requireActiveById(Integer.valueOf(username));
            }
            return usersService.requireActiveByEmail(username);
        }
        String username = principal.toString();
        if (username.matches("\\d+")) {
            Integer id = Integer.valueOf(username);
            return usersService.requireActiveById(id);
        }
        return usersService.requireActiveByEmail(username);
    }
}
