package com.example.capstonedesign.domain.users.controller;

import com.example.capstonedesign.common.exception.ApiException;
import com.example.capstonedesign.common.exception.ErrorCode;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * UsersController
 * -------------------------------------------------
 * - 회원 가입/로그인/토큰 기반 자기 정보 조회·수정·탈퇴 등을 제공
 * - JWT는 Authorization 헤더("Bearer <token>")로 전달받아 수동 파싱
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "회원 가입/로그인/내 정보 API")
public class UsersController {

    private final UsersService usersService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원 가입
     * - 가입 성공 시 생성된 사용자 정보를 반환
     * - 상태코드는 200(OK)로 반환 중
     */
    @Operation(
            summary = "회원 가입",
            description = "새로운 사용자를 생성합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "가입 성공",
                            content = @Content(schema = @Schema(implementation = UsersResponse.class))),
                    @ApiResponse(responseCode = "409", description = "이메일 중복",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @PostMapping("/signup")
    public ResponseEntity<UsersResponse> signup(@Valid @RequestBody SignupRequest req) {
        return ResponseEntity.ok(usersService.signup(req));
    }

    /**
     * 로그인 → JWT 발급
     * - 이메일로 사용자 조회 후, 비밀번호 일치 시 JWT 생성
     * - 실패 시 UNAUTHORIZED(401)
     */
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

        // (3) 토큰 발급 (subject=email, claim=role)
        String token = jwtTokenProvider.generate(user.getEmail(), user.getRole());
        long expiresIn = jwtTokenProvider.getExpirySeconds(); // 만료(초)

        // (4) 액세스 토큰 응답
        return ResponseEntity.ok(
                new TokenResponse(token, "Bearer", expiresIn)
        );
    }

    /**
     * 내 정보 조회 (JWT 필요)
     * - Authorization: Bearer <JWT>
     * - 토큰 subject(email)로 사용자 재조회
     */
    @Operation(
            summary = "내 정보 조회",
            description = "JWT의 subject(email)로 내 정보를 조회합니다.",
            security = @SecurityRequirement(name = "BearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = UsersResponse.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @GetMapping("/me")
    public ResponseEntity<UsersResponse> me(@org.springframework.security.core.annotation.AuthenticationPrincipal Object principal) {
        String email = (String) principal;
        var me = usersService.requireActiveByEmail(email);
        return ResponseEntity.ok(usersService.toResponse(me));
    }

    /**
     * 내 프로필 수정 (JWT 필요, 본인만)
     * - 토큰의 email을 신뢰 소스로 사용 (요청 바디에 email 받지 않음)
     */
    @Operation(
            summary = "내 프로필 수정",
            description = "전달된 필드만 부분 업데이트합니다.",
            security = @SecurityRequirement(name = "BearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공",
                            content = @Content(schema = @Schema(implementation = UsersResponse.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @PutMapping("/me")
    public ResponseEntity<UsersResponse> updateMe(@Valid @RequestBody UpdateUserRequest req) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UsersResponse updated = usersService.updateProfile(email, req);
        return ResponseEntity.ok(updated);
    }

    /**
     * 비밀번호 변경 (JWT 필요, 본인만)
     * - 현재 비밀번호 재확인 후 새 비밀번호로 교체
     */
    @Operation(
            summary = "비밀번호 변경",
            description = "현재 비밀번호를 검증한 뒤 새 비밀번호로 변경합니다.",
            security = @SecurityRequirement(name = "BearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "변경 성공",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패/현재 비밀번호 불일치",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @PatchMapping("/me/password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        String msg = usersService.changePassword(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(),
                req.currentPassword(),
                req.newPassword()
        );
        return ResponseEntity.ok(msg); // 200 OK + 메시지 반환
    }

    /**
     * 회원 탈퇴 (비밀번호 재확인)
     * - 보안상 권장: 토큰의 본인 email 사용 + 바디로 email 받지 않기
     * - 현재 구현은 요청 바디에 email+password를 받음 → 오용 여지 있음
     */
    @Operation(
            summary = "회원 탈퇴(소프트 삭제)",
            description = "비밀번호 재확인 후 deleted=true, deleted_at 기록.",
            security = @SecurityRequirement(name = "BearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "탈퇴 성공",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패/비밀번호 불일치",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @DeleteMapping("/delete")
    public ResponseEntity<String> delete(@Valid @RequestBody DeleteRequest req) {
        String msg = usersService.delete(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(),
                req.password()
        );
        return ResponseEntity.ok(msg); // 200 OK + 메시지 반환
    }

    // 오류 응답 스키마(예시). 실제 ApiException → ErrorCode 매핑 구조에 맞춰 수정 가능.
    @Schema(name = "ApiError", description = "에러 응답 포맷")
    static class ApiError {
        public String code;
        public String message;
        public ApiError() {}
        public ApiError(String code, String message) { this.code = code; this.message = message; }
    }
}
