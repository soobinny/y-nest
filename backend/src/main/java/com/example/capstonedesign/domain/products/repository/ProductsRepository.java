package com.example.capstonedesign.domain.products.repository;

import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * ProductsRepository
 * - Products 엔티티를 위한 Spring Data JPA 레포지토리 인터페이스
 * - 기본적인 CRUD 메서드는 JpaRepository 에서 상속받아 제공됨
 */
public interface ProductsRepository extends JpaRepository<Products, Integer> {

    /**
     * 특정 조건에 맞는 상품 조회
     * @param type 상품 유형 (HOUSING, FINANCE 등)
     * @param name 상품 이름
     * @param provider 제공자 (예: LH, 금융사 등)
     * @return Optional<Products> 조건에 해당하는 상품 (없을 수 있으므로 Optional로 반환)
     * <p>
     * - 메서드 이름 규칙에 따라 자동으로 쿼리 생성됨
     *   SELECT * FROM products
     *   WHERE type = ? AND name = ? AND provider = ?
     */
    Optional<Products> findByTypeAndNameAndProvider(ProductType type, String name, String provider);

    /**
     * 상세 페이지 URL로 상품 조회
     * -----------------------------------------------------
     * @param detailUrl 상품 상세 정보 페이지 URL
     * @return Optional<Products> — 해당 URL을 가진 상품이 존재할 경우 반환
     * <p>
     * 자동 생성되는 SQL 예시:
     * SELECT * FROM products
     * WHERE detail_url = ?
     */
    Optional<Products> findByDetailUrl(String detailUrl);

    List<Products> findTop5ByTypeAndNameContainingIgnoreCaseOrderByIdAsc(ProductType productType, String keywordLike);
}
