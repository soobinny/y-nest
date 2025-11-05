package com.example.capstonedesign.domain.finance.financeproducts.repository;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceLoanOption;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * FinanceLoanOptionRepository
 * - 대출 옵션(FinanceLoanOption) 엔티티 전용 JPA 리포지토리
 * - 금리, 옵션, 상품 타입별 필터링 및 분석용 JPQL 쿼리 포함
 */
public interface FinanceLoanOptionRepository extends JpaRepository<FinanceLoanOption, Integer> {

    /** 특정 금융상품에 속한 모든 대출 옵션 조회 */
    List<FinanceLoanOption> findByFinanceProduct(FinanceProducts financeProduct);

    /** 특정 금융상품의 대표 옵션 1건 조회 */
    FinanceLoanOption findTopByFinanceProduct(FinanceProducts product);

    /**
     * 대출 타입별 옵션 전체 조회
     * - loanTypes: 검색 대상 타입 목록 (예: [MORTGAGE_LOAN, RENT_HOUSE_LOAN])
     * - productType: 단일 타입 필터 (NULL 시 전체 검색)
     */
    @Query("""
            SELECT o FROM FinanceLoanOption o
            JOIN o.financeProduct p
            WHERE p.productType IN (:loanTypes)
              AND (:productType IS NULL OR p.productType = :productType)
            """)
    List<FinanceLoanOption> findLoansByType(
            @Param("productType") FinanceProductType productType,
            @Param("loanTypes") List<FinanceProductType> loanTypes
    );

    /** 금융상품 ID 기준 옵션 전체 삭제 (갱신 전 정리용) */
    void deleteByFinanceProductId(Integer financeProductId);

    /**
     * 대출 옵션 상세 검색 (다중 조건 + 페이징)
     * - 필터: 상품유형, 금리유형, 상환방식, 담보유형, 평균금리 범위, 키워드(상품명/제공사)
     * - NULL 파라미터는 무시되어 전체 검색 가능
     */
    @Query("""
            SELECT o FROM FinanceLoanOption o
            JOIN o.financeProduct fp
            JOIN fp.product p
            WHERE (:type IS NULL OR fp.productType = :type)
              AND (:lendType IS NULL OR o.lendTypeName = :lendType)
              AND (:rpayType IS NULL OR o.rpayTypeName = :rpayType)
              AND (:mrtgType IS NULL OR o.mrtgTypeName = :mrtgType)
              AND (:minRate IS NULL OR o.lendRateAvg >= :minRate)
              AND (:maxRate IS NULL OR o.lendRateAvg <= :maxRate)
              AND (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%')
                                OR p.provider LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<FinanceLoanOption> searchLoanOptions(
            @Param("type") FinanceProductType type,
            @Param("lendType") String lendType,
            @Param("rpayType") String rpayType,
            @Param("mrtgType") String mrtgType,
            @Param("minRate") BigDecimal minRate,
            @Param("maxRate") BigDecimal maxRate,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 평균 금리가 가장 낮은 대출상품 TOP N 조회
     * - 상품 단위로 그룹핑 후 MIN(lendRateAvg) 기준 정렬
     * - Projection 인터페이스로 개선 가능
     */
    @Query("""
            SELECT fp.id AS productId, p.name AS name, p.provider AS provider,
                   MIN(o.lendRateAvg) AS minAvgRate
            FROM FinanceLoanOption o
            JOIN o.financeProduct fp
            JOIN fp.product p
            WHERE fp.productType = :type
            GROUP BY fp.id, p.name, p.provider
            ORDER BY MIN(o.lendRateAvg) ASC
            """)
    List<Object[]> findTopByLowestAvgRate(@Param("type") FinanceProductType type, Pageable pageable);

    /** 기존 옵션과 동일한 “상품 + 상환방식 + 금리유형 + 담보유형” 조합을 찾아서 이전 금리값을 백업하는 데 사용 */
    Optional<FinanceLoanOption> findTopByFinanceProductAndRpayTypeNameAndLendTypeNameAndMrtgTypeName(
            FinanceProducts financeProduct,
            String rpayTypeName,
            String lendTypeName,
            String mrtgTypeName
    );
}
