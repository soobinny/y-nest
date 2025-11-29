package com.example.capstonedesign.domain.finance;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FinanceProductSpecs 단위 테스트
 * - 각 Specification 이 null / 비-null 조건에서 분기 없이 잘 동작하는지 검증
 * - JPA Criteria API 는 Mockito 로 mocking
 */
class FinanceProductSpecsTest {

    // -------------------------------
    // 0. private 생성자 커버리지용
    // -------------------------------
    @Test
    @DisplayName("유틸 클래스는 private 생성자로만 생성 가능하다(커버리지용)")
    void constructor_isPrivateAndCallableByReflection() throws Exception {
        Constructor<FinanceProductSpecs> ctor =
                FinanceProductSpecs.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        FinanceProductSpecs instance = ctor.newInstance();
        assertThat(instance).isNotNull();
    }

    // -------------------------------
    // 1. productType
    // -------------------------------
    @Test
    @DisplayName("productType - type 이 null 이면 조건이 적용되지 않는다")
    void productType_null_returnsNullPredicate() {
        Specification<FinanceProducts> spec =
                FinanceProductSpecs.productType(null);

        Predicate result = spec.toPredicate(
                mock(Root.class),
                mock(CriteriaQuery.class),
                mock(CriteriaBuilder.class)
        );

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("productType - type 이 주어지면 equal 조건을 생성한다")
    void productType_notNull_buildsPredicate() {
        // given
        FinanceProductType type = FinanceProductType.DEPOSIT;

        @SuppressWarnings("unchecked")
        Root<FinanceProducts> root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> cq = mock(CriteriaQuery.class);
        Predicate predicate = mock(Predicate.class);

        when(cb.equal(any(), eq(type))).thenReturn(predicate);

        // when
        Specification<FinanceProducts> spec =
                FinanceProductSpecs.productType(type);
        Predicate result = spec.toPredicate(root, cq, cb);

        // then
        assertThat(result).isSameAs(predicate);
        verify(cb, times(1)).equal(any(), eq(type));
    }

    // -------------------------------
    // 2. finCoNo
    // -------------------------------
    @Test
    @DisplayName("finCoNo - null 또는 공백이면 조건이 적용되지 않는다")
    void finCoNo_nullOrBlank_returnsNull() {
        Specification<FinanceProducts> specNull =
                FinanceProductSpecs.finCoNo(null);
        Specification<FinanceProducts> specBlank =
                FinanceProductSpecs.finCoNo("   ");

        Predicate r1 = specNull.toPredicate(
                mock(Root.class),
                mock(CriteriaQuery.class),
                mock(CriteriaBuilder.class)
        );
        Predicate r2 = specBlank.toPredicate(
                mock(Root.class),
                mock(CriteriaQuery.class),
                mock(CriteriaBuilder.class)
        );

        assertThat(r1).isNull();
        assertThat(r2).isNull();
    }

    @Test
    @DisplayName("finCoNo - 값이 있으면 equal 조건 생성")
    void finCoNo_valid_buildsPredicate() {
        String finCoNo = "001";

        @SuppressWarnings("unchecked")
        Root<FinanceProducts> root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> cq = mock(CriteriaQuery.class);
        Predicate predicate = mock(Predicate.class);

        when(cb.equal(any(), eq(finCoNo))).thenReturn(predicate);

        Specification<FinanceProducts> spec =
                FinanceProductSpecs.finCoNo(finCoNo);

        Predicate result = spec.toPredicate(root, cq, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).equal(any(), eq(finCoNo));
    }

    // -------------------------------
    // 3. keyword
    // -------------------------------
    @Test
    @DisplayName("keyword - null 또는 공백 키워드는 조건을 적용하지 않는다")
    void keyword_nullOrBlank_returnsNull() {
        Specification<FinanceProducts> specNull =
                FinanceProductSpecs.keyword(null);
        Specification<FinanceProducts> specBlank =
                FinanceProductSpecs.keyword("  ");

        Predicate r1 = specNull.toPredicate(
                mock(Root.class),
                mock(CriteriaQuery.class),
                mock(CriteriaBuilder.class)
        );
        Predicate r2 = specBlank.toPredicate(
                mock(Root.class),
                mock(CriteriaQuery.class),
                mock(CriteriaBuilder.class)
        );

        assertThat(r1).isNull();
        assertThat(r2).isNull();
    }

    @Test
    @DisplayName("keyword - name/provider LIKE 검색 + distinct(true) 설정")
    void keyword_valid_buildsLikePredicateAndDistinct() {
        String kw = "청년";

        @SuppressWarnings("unchecked")
        Root<FinanceProducts> root = mock(Root.class);
        CriteriaQuery<?> cq = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        // join(product)
        @SuppressWarnings("unchecked")
        Join<Object, Object> prodJoin = mock(Join.class);
        when(root.join(eq("product"), eq(JoinType.LEFT))).thenReturn(prodJoin);

        // product.name / product.provider path
        @SuppressWarnings("rawtypes")
        Path namePath = mock(Path.class);
        @SuppressWarnings("rawtypes")
        Path providerPath = mock(Path.class);
        when(prodJoin.get("name")).thenReturn(namePath);
        when(prodJoin.get("provider")).thenReturn(providerPath);

        // lower(name), lower(provider)
        @SuppressWarnings("unchecked")
        Expression<String> lowerName = mock(Expression.class);
        @SuppressWarnings("unchecked")
        Expression<String> lowerProvider = mock(Expression.class);
        when(cb.lower(namePath)).thenReturn(lowerName);
        when(cb.lower(providerPath)).thenReturn(lowerProvider);

        // like, or
        Predicate p1 = mock(Predicate.class);
        Predicate p2 = mock(Predicate.class);
        Predicate or = mock(Predicate.class);

        when(cb.like(eq(lowerName), anyString())).thenReturn(p1);
        when(cb.like(eq(lowerProvider), anyString())).thenReturn(p2);
        when(cb.or(p1, p2)).thenReturn(or);

        // distinct(true) → 체이닝 가능하도록
        doReturn(cq).when(cq).distinct(true);

        Specification<FinanceProducts> spec =
                FinanceProductSpecs.keyword(kw);

        Predicate result = spec.toPredicate(root, cq, cb);

        assertThat(result).isSameAs(or);
        verify(root).join("product", JoinType.LEFT);
        verify(cq).distinct(true);
        verify(cb).or(p1, p2);
    }

    // -------------------------------
    // 4. providers (includes/excludes)
    // -------------------------------
    @Test
    @DisplayName("providers - includes/excludes 둘 다 비어 있으면 조건 없음")
    void providers_noIncludesNoExcludes_returnsNull() {
        Specification<FinanceProducts> spec =
                FinanceProductSpecs.providers(null, null);

        Predicate result = spec.toPredicate(
                mock(Root.class),
                mock(CriteriaQuery.class),
                mock(CriteriaBuilder.class)
        );

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("providers - includes만 있을 때 IN 조건을 만든다")
    void providers_onlyIncludes_buildsInPredicate() {
        // given
        @SuppressWarnings("unchecked")
        Root<FinanceProducts> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        CriteriaQuery<FinanceProducts> cq = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("unchecked")
        Join<Object, Object> prod = mock(Join.class);
        when(root.join("product", JoinType.LEFT)).thenReturn(prod);

        @SuppressWarnings("unchecked")
        Expression<String> providerExpr = mock(Expression.class);
        when(cb.lower(prod.get("provider"))).thenReturn(providerExpr);

        Predicate inPredicate = mock(Predicate.class);

        // ✅ 여기서 NPE 안 나도록 수정
        // 1) 그냥 정상 케이스만 보고 싶으면:
        // List<String> includes = List.of("우리은행");
        // 2) null 포함 케이스까지 보고 싶으면:
        List<String> includes = java.util.Arrays.asList("우리은행", null, "   ");

        when(providerExpr.in(java.util.List.of("우리은행")))
                .thenReturn(inPredicate);

        // when
        var spec = FinanceProductSpecs.providers(includes, null);
        Predicate result = spec.toPredicate(root, cq, cb);

        // then
        assertThat(result).isSameAs(inPredicate);
    }

    @Test
    @DisplayName("providers - exclude만 있을 때 NOT IN 또는 NULL 허용 OR 조건 생성")
    void providers_onlyExcludes_buildsNotInOrNullPredicate() {
        List<String> includes = List.of(); // empty
        List<String> excludes = List.of("신한은행", "국민은행");

        @SuppressWarnings("unchecked")
        Root<FinanceProducts> root = mock(Root.class);
        CriteriaQuery<?> cq = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("unchecked")
        Join<Object, Object> prodJoin = mock(Join.class);
        when(root.join("product", JoinType.LEFT)).thenReturn(prodJoin);

        @SuppressWarnings("rawtypes")
        Path providerPath = mock(Path.class);
        when(prodJoin.get("provider")).thenReturn(providerPath);

        @SuppressWarnings("unchecked")
        Expression<String> providerExpr = mock(Expression.class);
        when(cb.lower(providerPath)).thenReturn(providerExpr);

        Predicate inPredicate = mock(Predicate.class);
        Predicate notIn = mock(Predicate.class);
        Predicate isNull = mock(Predicate.class);
        Predicate or = mock(Predicate.class);

        when(providerExpr.in(anyCollection())).thenReturn(inPredicate);
        when(cb.not(inPredicate)).thenReturn(notIn);
        when(cb.isNull(providerPath)).thenReturn(isNull);
        when(cb.or(notIn, isNull)).thenReturn(or);

        Specification<FinanceProducts> spec =
                FinanceProductSpecs.providers(includes, excludes);

        Predicate result = spec.toPredicate(root, cq, cb);

        assertThat(result).isSameAs(or);
        verify(providerExpr).in(anyCollection());
        verify(cb).not(inPredicate);
        verify(cb).isNull(providerPath);
    }

    @Test
    @DisplayName("providers - include/exclude 모두 있을 때 IN 또는 NOT IN OR + distinct(true)")
    void providers_bothIncludesAndExcludes_buildsOrPredicateAndDistinct() {
        List<String> includes = List.of("우리은행");
        List<String> excludes = List.of("기타은행");

        @SuppressWarnings("unchecked")
        Root<FinanceProducts> root = mock(Root.class);
        CriteriaQuery<?> cq = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("unchecked")
        Join<Object, Object> prodJoin = mock(Join.class);
        when(root.join("product", JoinType.LEFT)).thenReturn(prodJoin);

        @SuppressWarnings("rawtypes")
        Path providerPath = mock(Path.class);
        when(prodJoin.get("provider")).thenReturn(providerPath);

        @SuppressWarnings("unchecked")
        Expression<String> providerExpr = mock(Expression.class);
        when(cb.lower(providerPath)).thenReturn(providerExpr);

        @SuppressWarnings("unchecked")
        CriteriaBuilder.In<String> inIncludes = mock(CriteriaBuilder.In.class);
        @SuppressWarnings("unchecked")
        CriteriaBuilder.In<String> inExcludes = mock(CriteriaBuilder.In.class);

        when(providerExpr.in(anyCollection()))
                .thenReturn(inIncludes)    // 첫 호출: includes
                .thenReturn(inExcludes);   // 두 번째 호출: excludes

        Predicate notExcludes = mock(Predicate.class);
        Predicate or = mock(Predicate.class);

        when(cb.not(inExcludes)).thenReturn(notExcludes);
        when(cb.or(inIncludes, notExcludes)).thenReturn(or);

        // distinct(true)
        doReturn(cq).when(cq).distinct(true);

        Specification<FinanceProducts> spec =
                FinanceProductSpecs.providers(includes, excludes);

        Predicate result = spec.toPredicate(root, cq, cb);

        assertThat(result).isSameAs(or);
        verify(cq).distinct(true);
    }

    // -------------------------------
    // 5. minRate / maxRate
    // -------------------------------
    @Test
    @DisplayName("minRate - null 이면 조건 없음")
    void minRate_null_returnsNull() {
        Specification<FinanceProducts> spec =
                FinanceProductSpecs.minRate(null);

        Predicate result = spec.toPredicate(
                mock(Root.class),
                mock(CriteriaQuery.class),
                mock(CriteriaBuilder.class)
        );

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("minRate() - min이 null이 아니면 Predicate를 생성한다")
    void minRate_notNull_buildsPredicate() {
        // given
        @SuppressWarnings("unchecked")
        Root<FinanceProducts> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        CriteriaQuery<FinanceProducts> cq = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        // interestRate 필드 Path<BigDecimal> mock
        @SuppressWarnings("unchecked")
        Path<BigDecimal> ratePath = (Path<BigDecimal>) mock(Path.class);

        when(root.<BigDecimal>get("interestRate")).thenReturn(ratePath);

        BigDecimal min = new BigDecimal("2.0");
        jakarta.persistence.criteria.Predicate expected = mock(jakarta.persistence.criteria.Predicate.class);

        when(cb.<BigDecimal>greaterThanOrEqualTo(ratePath, min)).thenReturn(expected);

        Specification<FinanceProducts> spec = FinanceProductSpecs.minRate(min);

        // when
        var result = spec.toPredicate(root, cq, cb);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isSameAs(expected);

        // 호출 검증
        verify(root, times(1)).get("interestRate");
        verify(cb, times(1)).greaterThanOrEqualTo(ratePath, min);
    }


    @Test
    @DisplayName("maxRate - null 이면 조건 없음")
    void maxRate_null_returnsNull() {
        Specification<FinanceProducts> spec =
                FinanceProductSpecs.maxRate(null);

        Predicate result = spec.toPredicate(
                mock(Root.class),
                mock(CriteriaQuery.class),
                mock(CriteriaBuilder.class)
        );

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("maxRate() - max가 null이 아니면 Predicate를 생성한다")
    void maxRate_notNull_buildsPredicate() {
        // given
        @SuppressWarnings("unchecked")
        Root<FinanceProducts> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        CriteriaQuery<FinanceProducts> cq = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("rawtypes")
        Path ratePath = mock(Path.class);
        when(root.get("interestRate")).thenReturn(ratePath);

        BigDecimal max = new BigDecimal("3.0");
        Predicate expected = mock(Predicate.class);

        // 오버로드 모호성 없이 딱 이 케이스만 스텁
        when(cb.lessThanOrEqualTo(ratePath, max))
                .thenReturn(expected);

        Specification<FinanceProducts> spec = FinanceProductSpecs.maxRate(max);

        // when
        Predicate result = spec.toPredicate(root, cq, cb);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isSameAs(expected);

        // 실제로 우리가 의도한 메서드가 호출됐는지도 한번 더 체크
        verify(cb, times(1)).lessThanOrEqualTo(ratePath, max);
        verify(root, times(1)).get("interestRate");
    }

    // -------------------------------
    // 6. 전체 스펙 조합이 예외 없이 동작하는지(보너스)
    // -------------------------------
    @Test
    @DisplayName("여러 FinanceProductSpecs를 and로 조합해도 NPE 없이 동작한다")
    void combinedSpecs_doesNotThrow() {

        // --- 공통 Criteria mock ---
        @SuppressWarnings("unchecked")
        Root<FinanceProducts> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        CriteriaQuery<FinanceProducts> cq = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        // cb.equal(...) 은 어디서든 쓸 수 있도록 넓게 스텁
        Predicate equalPredicate = mock(Predicate.class);
        when(cb.equal(any(), any())).thenReturn(equalPredicate);

        // ---- keyword() 스펙에서 사용하는 join / lower / like / or / distinct ----

        // JOIN product
        @SuppressWarnings("unchecked")
        Join<Object, Object> prodJoin = mock(Join.class);
        when(root.join("product", JoinType.LEFT)).thenReturn(prodJoin);

        // product.name / product.provider
        @SuppressWarnings("rawtypes")
        Path namePath = mock(Path.class);
        @SuppressWarnings("rawtypes")
        Path providerPath = mock(Path.class);
        when(prodJoin.get("name")).thenReturn(namePath);
        when(prodJoin.get("provider")).thenReturn(providerPath);

        // lower(name), lower(provider)
        @SuppressWarnings("unchecked")
        Expression<String> lowerName = mock(Expression.class);
        @SuppressWarnings("unchecked")
        Expression<String> lowerProvider = mock(Expression.class);
        when(cb.lower(namePath)).thenReturn(lowerName);
        when(cb.lower(providerPath)).thenReturn(lowerProvider);

        // like(lower(..), "%적금%")
        Predicate likeName = mock(Predicate.class);
        Predicate likeProvider = mock(Predicate.class);
        when(cb.like(any(), anyString())).thenReturn(likeName, likeProvider);

        // or(likeName, likeProvider)
        Predicate orPredicate = mock(Predicate.class);
        when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(orPredicate);

        // distinct(true) → cq 리턴
        doReturn(cq).when(cq).distinct(true);

        // ---- minRate / maxRate 에서 사용하는 interestRate 경로 / 비교 ----

        @SuppressWarnings("rawtypes")
        Path ratePath = mock(Path.class);
        when(root.get("interestRate")).thenReturn(ratePath);

        Predicate ratePredicate = mock(Predicate.class);
        // interestRate >= min
        when(cb.greaterThanOrEqualTo(
                any(Expression.class),
                any(BigDecimal.class)
        )).thenReturn(ratePredicate);

        // interestRate <= max
        when(cb.lessThanOrEqualTo(
                any(Expression.class),
                any(BigDecimal.class)
        )).thenReturn(ratePredicate);

        // ---- 최종 and(...) 결합 ----

        Predicate andPredicate = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

        // ---- 실제 Specification 조합 ----
        Specification<FinanceProducts> spec =
                FinanceProductSpecs.productType(FinanceProductType.DEPOSIT)
                        .and(FinanceProductSpecs.finCoNo("001"))
                        .and(FinanceProductSpecs.keyword("적금"))
                        .and(FinanceProductSpecs.minRate(new BigDecimal("1.0")))
                        .and(FinanceProductSpecs.maxRate(new BigDecimal("5.0")));

        // --- 실행: 예외만 안 나면 성공 ---
        assertDoesNotThrow(() -> spec.toPredicate(root, cq, cb));
    }
}
