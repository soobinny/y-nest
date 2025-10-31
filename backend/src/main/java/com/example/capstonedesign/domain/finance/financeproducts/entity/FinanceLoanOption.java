package com.example.capstonedesign.domain.finance.financeproducts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FinanceLoanOption 엔티티
 * <p>
 * - 각 대출 상품(FinanceProducts)의 세부 금리·유형 옵션 정보를 저장
 * - 예: 고정/변동금리, 상환방식(원리금균등/만기일시), 담보유형(아파트/주택 등)
 * - 하나의 FinanceProducts(대출 상품)에 여러 옵션이 연결될 수 있음
 */
@Entity
@Table(name = "finance_loan_options",
        indexes = {
                @Index(name = "idx_flo_product", columnList = "finance_product_id"),
                @Index(name = "idx_flo_avg_rate", columnList = "lend_rate_avg"),
                @Index(name = "idx_flo_types_name", columnList = "lend_type_name, rpay_type_name, mrtg_type_name")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FinanceLoanOption {

    /** 기본 키 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 상위 금융상품(FinanceProducts)과의 연관 관계 (N:1)
     * - 외래키: finance_product_id
     * - fetch = LAZY → 필요 시 로딩
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finance_product_id", nullable = false)
    private FinanceProducts financeProduct;

    /** 금리 정보 */
    private BigDecimal lendRateMin;   // 최저 금리
    private BigDecimal lendRateMax;   // 최고 금리
    private BigDecimal lendRateAvg;   // 평균 금리

    /** 명칭 필드 (표시용) */
    private String rpayTypeName;      // 상환 유형명 (원리금균등/만기일시 등)
    private String lendTypeName;      // 금리 유형명 (고정/변동 등)
    private String mrtgTypeName;      // 담보 유형명 (주택, 아파트 등)

    // 신용등급별 금리 필드
    private String crdtLendRateType;       // 금리구분 코드
    private String crdtLendRateTypeNm;     // 금리구분명 (고정/변동)
    private BigDecimal crdtGrad1;          // 900점 초과
    private BigDecimal crdtGrad4;          // 801~900점
    private BigDecimal crdtGrad5;          // 701~800점
    private BigDecimal crdtGrad6;          // 601~700점
    private BigDecimal crdtGrad10;         // 501~600점
    private BigDecimal crdtGrad11;         // 401~500점
    private BigDecimal crdtGrad12;         // 301~400점
    private BigDecimal crdtGrad13;         // 300점 이하
    private BigDecimal crdtGradAvg;        // 평균금리

    /** 생성·수정 시각 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 자동 타임스탬프 처리 */
    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    /** 수정 시 updatedAt 자동 갱신 */
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}