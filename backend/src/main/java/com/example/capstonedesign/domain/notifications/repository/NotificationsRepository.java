package com.example.capstonedesign.domain.notifications.repository;

import com.example.capstonedesign.domain.notifications.entity.Notifications;
import com.example.capstonedesign.domain.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * NotificationRepository
 * ---------------------------------------------------------
 * - 사용자별 알림 내역 조회 및 상태 기반 검색 기능 제공
 * - Notification 엔티티의 CRUD 및 커스텀 쿼리 관리
 */
@Repository
public interface NotificationsRepository extends JpaRepository<Notifications, Long> {

    /**
     * 특정 사용자의 알림 전체 조회 (최신순)
     */
    List<Notifications> findByUserOrderByCreatedAtDesc(Users user);

    /**
     * 알림 상태로 조회 (예: SENT / FAILED)
     */
    List<Notifications> findByStatus(String status);

    /**
     * 알림 타입별 조회 (예: EMAIL / KAKAO / SMS)
     */
    List<Notifications> findByType(String type);
}
