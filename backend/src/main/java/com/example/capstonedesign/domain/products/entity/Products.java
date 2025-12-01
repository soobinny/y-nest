package com.example.capstonedesign.domain.products.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Products 엔티티
 * - 금융 상품과 주거상 품을 아우르는 공통 상품 정보를 저장하는 테이블 매핑
 * - 각 상품은 type(주거/금융), 이름, 제공기관, 상세 URL 등을 가짐
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "products")
public class Products {

    /** 기본 키 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 상품 유형
     * - HOUSING, FINANCE 등 ENUM 값으로 관리
     * - EnumType.STRING 으로 DB 에는 문자열 형태로 저장
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType type;

    /** 상품 이름 (NULL 불가) */
    @Column(nullable = false)
    private String name;

    /** 상품 제공자 (예: LH, 금융사 등) */
    @Column(length = 100)
    private String provider;

    /** 상세 페이지 URL (최대 500자) */
    @Column(name = "detail_url", length = 500)
    private String detailUrl;
}
