package com.example.capstonedesign.domain.finance.financeproducts.entity;

import com.example.capstonedesign.domain.products.entity.Products;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * FinanceProducts 엔티티
 * - 금융상품(예금/적금 등)의 상세 정보를 저장
 * - Products 엔티티와 N:1 관계 (공통 상품 정보와 연결됨)
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "finance_products")
public class FinanceProducts {

    /** 기본 키 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 공통 상품(Products)과의 연관 관계 (N:1)
     * - 하나의 금융상품은 반드시 하나의 Products에 속함
     * - 외래키: product_id
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    /**
     * 금융 회사 고유 번호
     * - 금감원 API 에서 내려주는 금융사 식별 코드
     * - NOT NULL, 최대 20자
     */
    @Column(name = "fin_co_no", nullable = false, length = 20)
    private String finCoNo;

    /**
     * 금융 상품 유형 (예금/적금)
     * - EnumType.STRING 으로 DB 에는 문자열 저장
     * - 예: DEPOSIT, SAVING
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private FinanceProductType productType;

    /**
     * 가입 조건 (문자열이 길 수 있어 @Lob 처리)
     * - 예: "만 19세 이상, 신규 가입자만 가능"
     */
    @Lob
    private String join_condition;

    /**
     * 대표 금리
     * - precision = 5, scale = 2 → 최대 999.99 까지 저장 가능
     * - 예: 3.50 (%)
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal interest_rate;

    /**
     * 최소 예치금 (단위: 원)
     * - nullable 허용
     * - 예: 1000000 (100만원)
     */
    private Integer min_deposit;
}
