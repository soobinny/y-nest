package com.example.capstonedesign.domain.users.repository;

import com.example.capstonedesign.domain.users.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * PasswordResetTokenRepository
 * -------------------------------------------------
 * - {@link PasswordResetToken} 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리
 * - 비밀번호 재설정 토큰 조회 및 관리 기능 제공
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * 토큰 문자열로 비밀번호 재설정 토큰 조회
     *
     * @param token 비밀번호 재설정 토큰 문자열
     * @return 해당 토큰 정보 (Optional)
     */
    Optional<PasswordResetToken> findByToken(String token);
}
