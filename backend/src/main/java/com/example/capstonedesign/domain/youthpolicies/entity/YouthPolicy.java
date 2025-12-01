package com.example.capstonedesign.domain.youthpolicies.entity;

import com.example.capstonedesign.domain.products.entity.Products;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * YouthPolicy
 * -------------------------------------------------
 * 청년정책 정보 엔티티 (온통청년 API 기반)
 * - 정책명, 설명, 카테고리, 기관, 지원대상 등 정책 세부정보 저장
 */
@Entity
@Table(name = "youth_policies")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class YouthPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    /** 정책 고유번호 (온통청년 plcyNo) */
    @Column(unique = true, nullable = false)
    private String policyNo;

    private String policyName;       // 정책명
    @Column(length = 1000)
    private String description;      // 정책 설명
    private String keyword;          // 키워드
    private String categoryLarge;    // 대분류
    private String categoryMiddle;   // 중분류
    private String agency;           // 주관기관
    private String applyUrl;         // 신청 URL
    private String regionCode;       // 지역 코드
    private String targetAge;        // 대상 연령
    private String supportContent;   // 지원 내용
    private String startDate;        // 시작일
    private String endDate;          // 종료일

    private LocalDateTime createdAt; // 생성일
    private LocalDateTime updatedAt; // 수정일

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
