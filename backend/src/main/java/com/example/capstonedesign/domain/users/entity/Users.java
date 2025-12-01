package com.example.capstonedesign.domain.users.entity;

import com.example.capstonedesign.domain.notifications.entity.NotificationChannel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Users 엔티티
 * -------------------------------------------------
 * - 사용자 정보를 담는 엔티티 클래스
 * - DB 테이블은 'users' 로 매핑되며, 사용자에 대한 주요 정보를 관리
 * - 회원 가입, 프로필 수정, 탈퇴 등의 기능에서 사용
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    /** 사용자 고유 ID */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 사용자 이메일 (유니크, 필수) */
    @Column(nullable = false, unique = true)
    private String email;

    /** 사용자 비밀번호 (필수) */
    @Column(nullable = false)
    private String password;

    /** 사용자 이름 (nullable) */
    private String name;

    /** 사용자 나이 (nullable) */
    private Integer age;

    /** 소득 구간 (nullable, 최대 길이 50) */
    @Column(length = 50)
    private String income_band;

    /** 거주 지역 (nullable, 최대 길이 50) */
    @Column(length = 50)
    private String region;

    /** 무주택 여부 (기본값 false) */
    @Column(nullable = false)
    private Boolean is_homeless = false;

    @Column(name = "birthdate")
    private LocalDate birthdate;

    /** 사용자 역할 (기본값 USER, ENUM: USER/OWNER) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserRole role = UserRole.USER;

    /** 탈퇴 여부 (기본값 false) */
    @Column(nullable = false)
    private Boolean deleted = false;

    /** 탈퇴 일시 (nullable) */
    private Instant deleted_at;

    /** 사용자 생성 일시 (기본값 현재 시간) */
    @Column(nullable = false)
    private Instant created_at = Instant.now();

    /** 사용자 정보 수정 일시 (기본값 현재 시간) */
    @Column(nullable = false)
    private Instant updated_at = Instant.now();

    /** 사용자 알림 수신 여부 */
    @Column(nullable = false)
    private Boolean notificationEnabled = true;  // 기본값 true

    /** 사용자 알림 채널(이메일, 카카오톡, 문자) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel notificationChannel = NotificationChannel.EMAIL; // 기본값 이메일

    /**
     * 엔티티 업데이트 시 updated_at 자동 갱신
     * - 엔티티가 변경될 때마다 갱신 시간 자동 설정
     */
    @PreUpdate
    void onUpdate() {
        this.updated_at = Instant.now();
    }
}
