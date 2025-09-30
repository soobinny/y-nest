package com.example.capstonedesign.domain.finance.financeproducts.repository;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.products.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * FinanceProductsRepository
 * - FinanceProducts 엔티티를 위한 Spring Data JPA 레포지토리
 * - JpaRepository: 기본 CRUD, 페이징, 정렬 기능 제공
 * - JpaSpecificationExecutor: 동적 조건 검색(Specification) 지원
 */
public interface FinanceProductsRepository extends JpaRepository<FinanceProducts, Integer>,
        JpaSpecificationExecutor<FinanceProducts> {

    /**
     * 특정 상품 + 금융 회사 번호(finCoNo) 기준으로 금융 상품 조회
     *
     * @param product  Products 엔티티 (상품 기본 정보)
     * @param finCoNo  금융 회사 고유 번호
     * @return Optional<FinanceProducts> 조건에 해당하는 금융 상품 (없을 수 있으므로 Optional로 반환)
     * <p>
     * - 메서드 네이밍 규칙에 의해 자동으로 JPQL 쿼리 생성
     *   SELECT * FROM finance_products
     *   WHERE product_id = ? AND fin_co_no = ?
     */
    Optional<FinanceProducts> findByProductAndFinCoNo(Products product, String finCoNo);
}
