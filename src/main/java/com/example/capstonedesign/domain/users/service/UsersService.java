package com.example.capstonedesign.domain.users.service;

import com.example.capstonedesign.common.exception.ApiException;
import com.example.capstonedesign.common.exception.ErrorCode;
import com.example.capstonedesign.domain.users.config.PasswordEncoder;
import com.example.capstonedesign.domain.users.dto.request.SignupRequest;
import com.example.capstonedesign.domain.users.dto.request.UpdateUserRequest;
import com.example.capstonedesign.domain.users.dto.response.UsersResponse;
import com.example.capstonedesign.domain.users.entity.UserRole;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * UsersService
 * -------------------------------------------------
 * - 사용자 관련 핵심 비즈니스 로직 제공
 * - 회원가입, 로그인 관련 조회, 프로필 수정, 비밀번호 변경, 회원 탈퇴 처리
 * - 트랜잭션(@Transactional) 적용으로 일관성 보장
 */
@Service
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

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
