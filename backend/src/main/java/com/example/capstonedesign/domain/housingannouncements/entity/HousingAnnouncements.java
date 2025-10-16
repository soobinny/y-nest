package com.example.capstonedesign.domain.housingannouncements.entity;

import com.example.capstonedesign.domain.products.entity.Products;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * HousingAnnouncements
 * -----------------------------------------------------
 * - LH(한국토지주택공사) 등에서 제공하는 주거 관련 공고 정보를 저장하는 엔티티
 * - 분양, 임대, 정정공고 등 다양한 유형의 주거 공고 데이터를 관리함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "housing_announcements")
public class HousingAnnouncements {

    /** 기본 키 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연관된 상품 정보
     * -----------------------------------------------------
     * - 하나의 주거 공고는 반드시 하나의 상품(Products)에 속함
     * - product_id는 FK 이며, 공고당 상품은 유일(unique = true)
     */
    @OneToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Products product;

    /** 공고 대상 지역명 (예: 서울특별시, 부산광역시 등) */
    @Column(name = "region_name")
    private String regionName;

    /** 공고 게시일 (공고가 시작된 날짜) */
    @Column(name = "notice_date")
    private LocalDate noticeDate;

    /** 공고 마감일 (접수 마감 또는 종료일) */
    @Column(name = "close_date")
    private LocalDate closeDate;

    /**
     * 공고 진행 상태
     * -----------------------------------------------------
     * 예: 공고중, 접수중, 정정공고중, 접수마감, 종료
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private HousingStatus status;

    /**
     * 공고 카테고리
     * -----------------------------------------------------
     * 예: 임대주택, 분양주택
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private HousingCategory category;

    /** 생성 시각 (자동 기록) */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /** 마지막 수정 시각 (자동 업데이트) */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * Product 만으로 공고 객체를 생성할 때 사용하는 생성자
     * - 주로 데이터 수집 시 상품과 공고를 1:1 매핑할 때 사용
     */
    public HousingAnnouncements(Products product) {
        this.product = product;
    }
}
