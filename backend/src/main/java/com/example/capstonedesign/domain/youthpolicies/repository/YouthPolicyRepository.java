package com.example.capstonedesign.domain.youthpolicies.repository;

import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * YouthPolicyRepository
 * -------------------------------------------------
 * 청년정책(YouthPolicy) 엔티티용 JPA 리포지토리
 * - 단건 조회 및 기간 조건 기반 정책 조회 기능 제공
 */
public interface YouthPolicyRepository extends JpaRepository<YouthPolicy, Long> {

    /**
     * 정책 고유번호로 단건 조회
     * 예: 정책 상세 페이지 진입 시 사용
     */
    Optional<YouthPolicy> findByPolicyNo(String policyNo);

    /**
     * 종료되지 않은 정책만 조회 (진행 중 + 예정)
     * - 종료일(endDate) ≥ 오늘(:today)
     * - 시작일(startDate) 최신순 정렬
     * - Pageable로 최대 개수 제한 가능
     */
    @Query("""
        SELECT y
        FROM YouthPolicy y
        WHERE y.endDate >= :today
        ORDER BY y.startDate DESC
    """)
    List<YouthPolicy> findActiveOrderByStartDateDesc(@Param("today") String today, Pageable pageable);
}
