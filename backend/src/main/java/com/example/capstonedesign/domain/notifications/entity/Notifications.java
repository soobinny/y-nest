package com.example.capstonedesign.domain.notifications.entity;

import com.example.capstonedesign.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications")
public class Notifications {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림 대상 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /** 알림 유형 (EMAIL, KAKAO, SMS 등) */
    @Column(nullable = false)
    private String type;

    /** 알림 상태 (SENT, FAILED 등) */
    @Column(nullable = false)
    private String status;

    /** 알림 메시지 본문 */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** 생성 시각 */
    @CreationTimestamp
    private Instant createdAt;
}
