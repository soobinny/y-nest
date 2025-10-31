package com.example.capstonedesign.domain.youthpolicies.repository;

import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * YouthPolicyRepository
 * -------------------------------------------------
 * - 청년정책(YouthPolicy) 엔티티의 데이터 접근 계층
 * - 정책 고유번호(policyNo)로 단건 조회 기능 제공
 */
public interface YouthPolicyRepository extends JpaRepository<YouthPolicy, Long> {
    Optional<YouthPolicy> findByPolicyNo(String policyNo);
}
