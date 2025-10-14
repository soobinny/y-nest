package com.example.capstonedesign.domain.users.service;

import com.example.capstonedesign.common.exception.ApiException;
import com.example.capstonedesign.common.exception.ErrorCode;
import com.example.capstonedesign.domain.users.config.PasswordEncoder;
import com.example.capstonedesign.domain.users.dto.request.SignupRequest;
import com.example.capstonedesign.domain.users.dto.request.UpdateUserRequest;
import com.example.capstonedesign.domain.users.dto.response.FindIdResponse;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.time.LocalDate;

/**
 * UsersService
 * -------------------------------------------------
 * - 사용자 관련 핵심 비즈니스 로직 제공
 * - 회원 가입, 로그인 관련 조회, 프로필 수정, 비밀번호 변경, 회원 탈퇴 처리
 * - 트랜잭션(@Transactional) 적용으로 일관성 보장
 */
@Service
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository prtRepository;
    private final EmailSender emailSender;

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(15);

    /**
     * 회원 가입
     * - 이메일 중복 체크 → 409 CONFLICT 예외 발생
     * - 비밀번호는 BCrypt로 암호화
     * - role null 대비 기본값 USER 적용
     *
     * @param req SignupRequest
     * @return 가입 완료 후 UsersResponse
     */
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
        u.setRole(req.role() != null ? req.role() : UserRole.USER);
        u.setBirthdate(req.birthdate()); 

        Users saved = usersRepository.save(u);
        return toResponse(saved);
        }


    /**
     * Users 엔티티 → UsersResponse DTO 변환
     */
    public UsersResponse toResponse(Users u) {
        return new UsersResponse(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getAge(),
                u.getIncome_band(),
                u.getRegion(),
                u.getIs_homeless(),                
                u.getBirthdate(),
                u.getRole()
        );
    }

    /**
     * 삭제되지 않은 활성 사용자 조회
     * - 이메일 기준
     * - 없으면 UNAUTHORIZED 예외 발생
     */
    public Users requireActiveByEmail(String email) {
        return usersRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Transactional
    public Users requireActiveById(Integer id) {
        Users u = usersRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));
        if (u.getDeleted()) throw new ApiException(ErrorCode.UNAUTHORIZED, "Inactive user");
        return u;
    }

    /**
     * 프로필 수정
     * - 전달된 필드가 null이 아닌 경우만 업데이트
     * - updated_at은 @PreUpdate 에서 자동 갱신
     *
     * @param email 사용자 이메일 (JWT subject)
     * @param req UpdateUserRequest
     * @return 업데이트 후 UsersResponse
     */
    @Transactional
    public UsersResponse updateProfile(String email, UpdateUserRequest req) {
        Users u = requireActiveByEmail(email);

        if (req.age() != null) u.setAge(req.age());
        if (req.income_band() != null) u.setIncome_band(req.income_band());
        if (req.region() != null) u.setRegion(req.region());
        if (req.is_homeless() != null) u.setIs_homeless(req.is_homeless());
        if (req.birthdate() != null) u.setBirthdate(req.birthdate());

        Users saved = usersRepository.save(u);
        return toResponse(saved);
    }

    /**
     * 비밀번호 변경
     * - 현재 비밀번호 검증 후 새 비밀번호로 변경
     *
     * @param email 사용자 이메일
     * @param currentPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     */
    @Transactional
    public String changePassword(String email, String currentPassword, String newPassword) {
        Users u = requireActiveByEmail(email);
        if (!passwordEncoder.matches(currentPassword, u.getPassword())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }
        u.setPassword(passwordEncoder.encode(newPassword));
        return "비밀번호가 성공적으로 변경되었습니다.";
    }

    /**
     * 아이디(이메일) 찾기
     * - 이름과 지역으로 활성 사용자를 조회하고, 마스킹된 이메일 목록을 반환
     *
     * @param name   사용자 이름
     * @param region 사용자 지역
     * @return 마스킹된 이메일 목록
     */
    public FindIdResponse findIdsByNameAndRegion(String name, String region) {
        List<Users> list = usersRepository.findActiveByNameAndRegion(name, region);

        // 일치하는 사용자가 없을 때 404 JSON 에러
        if (list.isEmpty()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "이름과 지역이 일치하는 사용자가 없습니다.");
        }

        List<String> masked = new ArrayList<>();
        for (Users u : list) masked.add(maskEmail(u.getEmail()));
        return new FindIdResponse(masked);
    }

    /**
     * 이메일 마스킹 처리
     * - 개인정보 보호를 위해 일부만 노출
     *
     * @param email 원본 이메일 주소
     * @return 마스킹된 이메일 문자열
     */
    // 간단한 마스킹 유틸 (a***@g***.com 형태)
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        String domainMain = domain;
        String domainTld = "";
        int lastDot = domain.lastIndexOf('.');
        if (lastDot > 0) {
            domainMain = domain.substring(0, lastDot);
            domainTld = domain.substring(lastDot); // .com 등
        }
        String maskedLocal = local.charAt(0) + "***";
        String maskedDomain = (domainMain.isEmpty() ? "*" : domainMain.charAt(0)) + "***";
        return maskedLocal + "@" + maskedDomain + (domainTld.isEmpty() ? "" : domainTld);
    }

    /**
     * 비밀번호 재설정 요청
     * - 사용자의 이메일로 비밀번호 재설정 토큰을 발급 및 메일 전송
     * - 보안상 존재하지 않는 이메일이어도 동일한 응답을 반환 (계정 열거 방지)
     *
     * @param email 비밀번호 재설정을 요청한 사용자 이메일
     */
    @Transactional
    public void requestPasswordReset(String email) {
        // 열거 방지: 존재 여부에 관계없이 동일한 메시지 반환 예정
        usersRepository.findByEmailAndDeletedFalse(email).ifPresent(user -> {
            String token = generateSecureToken();
            PasswordResetToken prt = new PasswordResetToken();
            prt.setUserId(user.getId());
            prt.setToken(token);
            prt.setExpiresAt(Instant.now().plus(RESET_TOKEN_TTL));
            prtRepository.save(prt);

            // 실제 서비스에선 reset URL 포함
            String resetUrl = "https://your-frontend.example.com/reset-password?token=" + token;
            emailSender.send(
                    user.getEmail(),
                    "[Capstone] 비밀번호 재설정 안내",
                    "아래 링크에서 비밀번호를 재설정하세요. (15분 유효)\n" + resetUrl
            );
        });
        // 존재 여부 상관없이 컨트롤러는 200 OK로 응답하도록
    }

    /**
     * 비밀번호 재설정 확정
     * - 토큰 검증 후 새 비밀번호로 교체하고, 토큰을 사용 처리
     *
     * @param token       비밀번호 재설정 토큰
     * @param newPassword 새 비밀번호
     */
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
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Inactive user");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        prt.setUsed(true);
        // 저장은 트랜잭션 종료 시 flush
    }

    // 보안 토큰 생성: 32바이트 난수 → URL-safe Base64
    private String generateSecureToken() {
        byte[] buf = new byte[32];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    /**
     * 회원 탈퇴 (소프트 삭제)
     * - 비밀번호 검증 후 deleted=true, deleted_at 기록
     *
     * @param email 사용자 이메일
     * @param rawPassword 입력 비밀번호
     */
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
}
