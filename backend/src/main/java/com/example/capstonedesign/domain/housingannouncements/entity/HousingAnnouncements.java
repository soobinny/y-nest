package com.example.capstonedesign.domain.housingannouncements.entity;

import com.example.capstonedesign.domain.products.entity.Products;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * HousingAnnouncements 엔티티
 * - 주거 관련 공고(분양, 임대 등)를 저장하는 테이블 매핑
 * - Products 테이블과 다대일(N:1) 관계를 가짐
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "housing_announcements")
public class HousingAnnouncements {

    /** 기본 키 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 연관된 상품 (Products 엔티티)
     * - 하나의 공고는 반드시 하나의 상품에 속함
     * - product_id FK로 매핑
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    /** 지역명 (예: 서울특별시, 부산광역시 등) */
    private String region_name;

    /** 공고 게시일 */
    private LocalDate notice_date;

    /** 공고 마감일 */
    private LocalDate close_date;

    /** 공고 상태 (예: 진행중, 마감 등) */
    private String status;

    /** 공고 카테고리 (예: 임대, 분양 등) */
    private String category;
}
