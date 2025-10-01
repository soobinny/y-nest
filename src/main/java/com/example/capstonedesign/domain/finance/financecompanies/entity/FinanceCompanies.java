package com.example.capstonedesign.domain.finance.financecompanies.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * FinanceCompanies 엔티티
 * - 금융 회사(은행, 보험사 등) 정보를 저장하는 테이블 매핑
 * - 각 금융 상품(FinanceProducts)은 이 회사를 참조함
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "finance_companies")
public class FinanceCompanies {

    /** 기본 키 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 금융 회사 고유 번호
     * - 금감원/외부 API 에서 제공하는 회사 식별자
     * - NOT NULL, UNIQUE 제약 조건
     * - 최대 길이: 20자
     */
    @Column(name = "fin_co_no", nullable = false, unique = true, length = 20)
    private String finCoNo;

    /**
     * 금융 회사 이름
     * - 예: KB 국민은행, 신한은행, 삼성생명 등
     * - NOT NULL
     * - 최대 길이: 100자
     */
    @Column(nullable = false, length = 100)
    private String name;

    /** 금융 회사 홈페이지 URL */
    private String homepage;

    /** 금융 회사 연락처 (대표 번호 등) */
    private String contact;
}
