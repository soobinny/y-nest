package com.example.capstonedesign.domain.finance.financeproducts.repository;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.products.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * FinanceProductsRepository
 * - 금융 상품(FinanceProducts) 엔티티 전용 Repository
 * - 기본 CRUD + 상품/회사 기반 조회 + 타입별 필터링 제공
 * - JpaSpecificationExecutor: 동적 검색(Specification) 지원
 */
public interface FinanceProductsRepository extends JpaRepository<FinanceProducts, Integer>,
        JpaSpecificationExecutor<FinanceProducts> {

    /**
     * 특정 상품 + 금융회사 번호로 금융상품 조회
     * - 존재하지 않을 수 있으므로 Optional 반환
     * - JPA 메서드 네이밍 규칙으로 자동 쿼리 생성
     */
    Optional<FinanceProducts> findByProductAndFinCoNo(Products product, String finCoNo);

    /** 특정 금융상품 타입별 전체 조회 (예: 예금, 적금, 대출 등) */
    List<FinanceProducts> findByProductType(FinanceProductType financeProductType);
}
