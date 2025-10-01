package com.example.capstonedesign.domain.finance.financecompanies.repository;

import com.example.capstonedesign.domain.finance.financecompanies.entity.FinanceCompanies;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * FinanceCompaniesRepository
 * - FinanceCompanies 엔티티를 위한 Spring Data JPA 레포지토리 인터페이스
 * - 기본 CRUD, 페이징/정렬 기능은 JpaRepository 에서 상속받아 사용 가능
 */
public interface FinanceCompaniesRepository extends JpaRepository<FinanceCompanies, Integer> {

    /**
     * 금융 회사 고유 번호(fin_co_no)로 회사 조회
     * @param finCoNo 금융 회사 고유 식별자 (금감원/외부 API 제공 값)
     * @return Optional<FinanceCompanies> 해당 회사가 존재하면 반환, 없으면 빈 Optional
     * <p>
     * - 메서드 네이밍 규칙에 의해 자동으로 JPQL 쿼리 생성
     *   SELECT * FROM finance_companies WHERE fin_co_no = ?
     */
    Optional<FinanceCompanies> findByFinCoNo(String finCoNo);
}
