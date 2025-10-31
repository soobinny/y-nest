package com.example.capstonedesign.domain.housingannouncements.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * LH 공고 엔티티 (LhNotice)
 * ---------------------------------------------------------
 * - LH(한국토지주택공사)에서 제공하는 임대/분양 공고 정보를 저장
 * - panNm(공고명) + panNtStDt(게시일) 조합으로 중복 방지
 */
@Entity
@Table(name = "lh_notices", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"panNm", "panNtStDt"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LhNotice {

    /** 기본키 (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 상위 공고유형명 (예: 임대공고, 분양공고) */
    private String uppAisTpNm;

    /** 세부 공고유형명 (예: 국민임대, 행복주택 등) */
    private String aisTpCdNm;

    /** 공고명 (예: 2025년 행복주택 입주자 모집공고) */
    private String panNm;

    /** 지역명 (예: 서울특별시, 부산광역시 등) */
    private String cnpCdNm;

    /** 공고 상태 (예: 공고중, 접수중, 정정공고중 등) */
    private String panSs;

    /** 공고 게시일 (문자열 형태, yyyy-MM-dd) */
    private String panNtStDt;

    /** 공고 마감일 (문자열 형태, yyyy-MM-dd) */
    private String clsgDt;

    /** 상세 조회 URL */
    private String dtlUrl;

    /** 데이터 생성 시각 */
    private LocalDateTime createdAt = LocalDateTime.now();
}
