package com.example.capstonedesign.domain.users.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * PasswordResetToken
 * -------------------------------------------------
 * - 비밀번호 재설정을 위한 토큰 정보를 저장하는 엔티티
 * - 각 토큰은 단 한 번만 사용 가능하며, 일정 시간(예: 15분) 후 만료됨
 * - 사용자 ID를 직접 저장(FK)하여 Users 엔티티와 연관
 */
@Entity
@Getter @Setter
@Table(name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_prt_token", columnList = "token", unique = true),
                @Index(name = "idx_prt_userid", columnList = "user_id")
        })
public class PasswordResetToken {

    /** 기본 키 (자동 증가) */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사용자 ID (Users.id 참조) */
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    /** 재설정 토큰 (URL-safe Base64 문자열) */
    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;

    /** 토큰 만료 시각 */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** 사용 여부 (true = 이미 사용됨) */
    @Column(name = "used", nullable = false)
    private boolean used = false;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}