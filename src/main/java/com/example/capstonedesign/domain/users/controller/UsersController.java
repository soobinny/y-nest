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
public class UsersController {

    private final UsersService usersService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원 가입
     * - 가입 성공 시 생성된 사용자 정보를 반환
     * - 상태코드는 200(OK)로 반환 중
     */
    @PostMapping("/signup")
    public ResponseEntity<UsersResponse> signup(@Valid @RequestBody SignupRequest req) {
        return ResponseEntity.ok(usersService.signup(req));
    }

    /**
     * 로그인 → JWT 발급
     * - 이메일로 사용자 조회 후, 비밀번호 일치 시 JWT 생성
     * - 실패 시 UNAUTHORIZED(401)
     */
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
    @GetMapping("/me")
    public ResponseEntity<UsersResponse> me() {
        // SecurityContext 에서 인증 정보 가져오기
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Users me = usersService.requireActiveByEmail(email);
        return ResponseEntity.ok(usersService.toResponse(me));
    }

    /**
     * 내 프로필 수정 (JWT 필요, 본인만)
     * - 토큰의 email을 신뢰 소스로 사용 (요청 바디에 email 받지 않음)
     */
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
    @DeleteMapping("/delete")
    public ResponseEntity<String> delete(@Valid @RequestBody DeleteRequest req) {
        String msg = usersService.delete(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString(),
                req.password()
        );
        return ResponseEntity.ok(msg); // 200 OK + 메시지 반환
    }
}
