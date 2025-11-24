package com.example.capstonedesign.domain.users.service;

import com.example.capstonedesign.common.exception.ApiException;
import com.example.capstonedesign.common.exception.ErrorCode;
import com.example.capstonedesign.domain.notifications.entity.NotificationChannel;
import com.example.capstonedesign.domain.users.config.PasswordEncoder;
import com.example.capstonedesign.domain.users.dto.request.SignupRequest;
import com.example.capstonedesign.domain.users.dto.request.UpdateUserRequest;
import com.example.capstonedesign.domain.users.dto.response.UsersResponse;
import com.example.capstonedesign.domain.users.entity.PasswordResetToken;
import com.example.capstonedesign.domain.users.entity.UserRole;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.port.EmailSender;
import com.example.capstonedesign.domain.users.repository.PasswordResetTokenRepository;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UsersService
 * -------------------------------------------------
 * - 회원 가입, 아이디 찾기, 비밀번호 재설정 등 사용자 관련 주요 비즈니스 로직 처리
 * - 인증 코드/토큰 생성 및 검증, 이메일 발송 포함
 */
@Service
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository prtRepository;
    private final EmailSender emailSender;

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration VERIFICATION_TTL = Duration.ofMinutes(5);

    private final Map<String, VerificationCode> verificationCodes = new ConcurrentHashMap<>();
    private record VerificationCode(String code, Instant expiresAt) {}

    // --------------------------------------------------------------------------
    // 1. 회원 가입
    // --------------------------------------------------------------------------
    @Transactional
    public UsersResponse signup(SignupRequest req) {
        if (usersRepository.existsByEmail(req.email())) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 가입된 이메일입니다.");
        }

        Users u = new Users();
        u.setEmail(req.email());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setName(req.name());
        u.setAge(req.age());
        u.setIncome_band(req.income_band());
        u.setRegion(req.region());
        u.setIs_homeless(req.is_homeless() != null ? req.is_homeless() : false);
        u.setNotificationEnabled(req.notificationEnabled() != null ? req.notificationEnabled() : true);
        u.setRole(req.role() != null ? req.role() : UserRole.USER);
        u.setBirthdate(req.birthdate());

        Users saved = usersRepository.save(u);
        return toResponse(saved);
    }

    // --------------------------------------------------------------------------
    // 2. 사용자 조회 관련 (로그인 등에서 사용)
    // --------------------------------------------------------------------------
    public Users requireActiveByEmail(String email) {
        return usersRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "이메일을 찾을 수 없습니다."));
    }

    public Users requireActiveById(Integer id) {
        return usersRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "활성화된 사용자가 아닙니다."));
    }

    // --------------------------------------------------------------------------
    // 3. 아이디(이메일) 찾기 - 인증 코드 전송 및 검증
    // ---------------------------------------------------------------------------
    @Transactional
    public void sendIdVerificationCode(String name, String email) {
        Optional<Users> userOpt = usersRepository.findByEmailAndDeletedFalse(email);
        if (userOpt.isEmpty() || !userOpt.get().getName().equals(name)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "입력하신 이름과 이메일이 일치하는 사용자가 없습니다.");
        }

        String code = generate6DigitCode();
        verificationCodes.put(email, new VerificationCode(code, Instant.now().plus(VERIFICATION_TTL)));

        String body = """
        안녕하세요, Y-Nest 본인 확인 서비스입니다.

        아이디(이메일) 찾기를 위한 인증 번호는 아래와 같습니다.

        인증 번호: %s
        유효 시간: 5분

        [어떻게 사용하나요?]
        1) Y-Nest 웹사이트에 접속합니다.
            📩 https://ynest.kro.kr/find-id
        2) 상단 메뉴에서 '아이디 찾기' 화면을 연 뒤,
        3) 이름과 이메일을 다시 입력하고,
        4) 화면에 나타나는 '인증 번호 입력' 칸에 위 인증 번호를 입력해 주세요.

        본인이 요청하지 않았다면 이 메일을 무시해 주세요.
        """.formatted(code);

        emailSender.send(
                email,
                "[Y-Nest] 아이디(이메일) 찾기 인증 번호 안내",
                body
        );
    }

    public String confirmIdVerification(String email, String code) {
        VerificationCode stored = verificationCodes.get(email);
        if (stored == null) throw new ApiException(ErrorCode.NOT_FOUND, "인증 요청 내역이 없습니다.");
        if (stored.expiresAt().isBefore(Instant.now())) {
            verificationCodes.remove(email);
            throw new ApiException(ErrorCode.UNAUTHORIZED, "인증 번호가 만료되었습니다.");
        }
        if (!stored.code().equals(code)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "인증 번호가 일치하지 않습니다.");
        }

        verificationCodes.remove(email);
        return email; // 인증 성공 시 이메일(아이디) 반환
    }

    // --------------------------------------------------------------------------
    // 4. 비밀번호 변경 / 재설정
    // -------------------------------------------------------------------------
    @Transactional
    public void requestPasswordReset(String email) {
        usersRepository.findByEmailAndDeletedFalse(email).ifPresent(user -> {
            String token = generateSecureToken();

            PasswordResetToken prt = new PasswordResetToken();
            prt.setUserId(user.getId());
            prt.setToken(token);
            prt.setExpiresAt(Instant.now().plus(RESET_TOKEN_TTL));
            prtRepository.save(prt);

            String resetUrl = "https://ynest.kro.kr/reset-password?token=" + token;
            emailSender.send(
                    user.getEmail(),
                    "[Y-Nest] 비밀번호 재설정 안내",
                    "아래 링크에서 비밀번호를 재설정하세요. (15분 유효)\n" + resetUrl
            );
        });
    }

    /** 비밀번호 재설정 완료 처리 */
    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        PasswordResetToken prt = prtRepository.findByToken(token)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다."));

        if (prt.isUsed() || prt.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "만료되었거나 이미 사용된 토큰입니다.");
        }

        Users u = usersRepository.findById(prt.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));

        if (Boolean.TRUE.equals(u.getDeleted())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "비활성화된 사용자입니다.");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        prt.setUsed(true);
    }

    // --------------------------------------------------------------------------
    // 5. 프로필 수정 / 탈퇴
    // --------------------------------------------------------------------------
    @Transactional
    public UsersResponse updateProfile(String email, UpdateUserRequest req) {
        Users u = requireActiveByEmail(email);

        if (req.age() != null) u.setAge(req.age());
        if (req.income_band() != null) u.setIncome_band(req.income_band());
        if (req.region() != null) u.setRegion(req.region());
        if (req.is_homeless() != null) u.setIs_homeless(req.is_homeless());
        if (req.notificationEnabled() != null) u.setNotificationEnabled(req.notificationEnabled());
        if (req.birthdate() != null) u.setBirthdate(req.birthdate());

        Users saved = usersRepository.save(u);
        return toResponse(saved);
    }

    @Transactional
    public void updateNotificationPreference(Integer userId, boolean enabled) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        user.setNotificationEnabled(enabled);
        usersRepository.save(user);
    }

    @Transactional
    public void updateNotificationChannel(Integer userId, NotificationChannel channel) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        user.setNotificationChannel(channel);
        usersRepository.save(user);
    }

    // --------------------------------------------------------------------------
    // 6. 회원 탈퇴
    // --------------------------------------------------------------------------
    @Transactional
    public String delete(String email, String rawPassword) {
        Users u = requireActiveByEmail(email);
        if (!passwordEncoder.matches(rawPassword, u.getPassword())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
        }
        u.setDeleted(true);
        u.setDeleted_at(Instant.now());
        return "회원 탈퇴가 완료되었습니다.";
    }

    // --------------------------------------------------------------------------
    // 유틸 메서드
    // --------------------------------------------------------------------------
    private String generate6DigitCode() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }

    private String generateSecureToken() {
        byte[] buf = new byte[32];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    public UsersResponse toResponse(Users u) {
        return new UsersResponse(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getAge(),
                u.getIncome_band(),
                u.getRegion(),
                u.getIs_homeless(),
                u.getNotificationEnabled(),
                u.getBirthdate(),
                u.getRole()
        );
    }
}
