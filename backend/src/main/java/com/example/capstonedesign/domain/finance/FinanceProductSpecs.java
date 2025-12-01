package com.example.capstonedesign.domain.finance;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * FinanceProductSpecs
 * - FinanceProducts 엔티티 검색 조건(Specification) 모음 클래스
 * - 동적 쿼리 생성을 위해 사용
 */
public final class FinanceProductSpecs {

    /** private 생성자로 인스턴스화 방지 (유틸 클래스) */
    private FinanceProductSpecs() {}

    /**
     * 금융 상품 유형으로 필터링
     * @param type DEPOSIT / SAVING
     * @return 해당 유형과 일치하는 조건
     */
    public static Specification<FinanceProducts> productType(FinanceProductType type) {
        return (root, q, cb) -> {
            if (type == null) return null; // 조건 미적용
            return cb.equal(root.get("productType"), type);
        };
    }

    /**
     * 금융 회사 고유 번호(finCoNo)로 필터링
     */
    public static Specification<FinanceProducts> finCoNo(String finCoNo) {
        return (root, q, cb) -> {
            if (finCoNo == null || finCoNo.isBlank()) return null;
            return cb.equal(root.get("finCoNo"), finCoNo);
        };
    }

    /**
     * 상품명(name) 또는 제공자(provider)에 키워드 검색 (LIKE)
     * - products 테이블 조인 필요
     * - 중복 제거를 위해 distinct(true) 설정
     */
    public static Specification<FinanceProducts> keyword(String kw) {
        return (root, q, cb) -> {
            if (kw == null || kw.isBlank()) return null;

            // LEFT JOIN finance_products.product p
            Join<Object, Object> prod = root.join("product", JoinType.LEFT);

            String like = "%" + kw.trim().toLowerCase() + "%";
            Objects.requireNonNull(q).distinct(true); // 조인으로 인한 중복 제거

            return cb.or(
                    cb.like(cb.lower(prod.get("name")), like),
                    cb.like(cb.lower(prod.get("provider")), like)
            );
        };
    }

    /**
     * 금융기관(provider) 다중 선택 필터
     * - includes: 선택한 은행 (IN)
     * - excludes: "그 외" 선택 시 제외할 은행 (NOT IN)
     * 두 조건은 OR 로 결합
     */
    public static Specification<FinanceProducts> providers(List<String> includes, List<String> excludes) {
        return (root, q, cb) -> {
            List<String> normalizedIncludes = normalize(includes);
            List<String> normalizedExcludes = normalize(excludes);

            boolean hasIncludes = !normalizedIncludes.isEmpty();
            boolean hasExcludes = !normalizedExcludes.isEmpty();

            if (!hasIncludes && !hasExcludes) {
                return null;
            }

            Join<Object, Object> prod = root.join("product", JoinType.LEFT);
            var providerExpr = cb.lower(prod.get("provider"));

            Objects.requireNonNull(q).distinct(true);

            if (hasIncludes && hasExcludes) {
                return cb.or(
                        providerExpr.in(normalizedIncludes),
                        cb.not(providerExpr.in(normalizedExcludes))
                );
            }

            if (hasIncludes) {
                return providerExpr.in(normalizedIncludes);
            }

            // only excludes ("그 외") selected -> provider not in excludes or null
            return cb.or(
                    cb.not(providerExpr.in(normalizedExcludes)),
                    cb.isNull(prod.get("provider"))
            );
        };
    }

    /**
     * 최소 금리 이상 검색
     */
    public static Specification<FinanceProducts> minRate(BigDecimal min) {
        return (root, q, cb) -> {
            if (min == null) return null;
            // 엔티티 필드명이 interestRate 라면 여기서도 camelCase로 써야 함!
            return cb.greaterThanOrEqualTo(root.get("interestRate"), min);
        };
    }

    /**
     * 최대 금리 이하 검색
     */
    public static Specification<FinanceProducts> maxRate(BigDecimal max) {
        return (root, q, cb) -> {
            if (max == null) return null;
            return cb.lessThanOrEqualTo(root.get("interestRate"), max);
        };
    }

    private static List<String> normalize(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
    }
}
