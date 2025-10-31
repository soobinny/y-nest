package com.example.capstonedesign.domain.housingannouncements.dto.response;

import com.example.capstonedesign.domain.housingannouncements.entity.HousingCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * HousingAnnouncementsResponse
 * -----------------------------------------------------
 * - 주거 공고(HousingAnnouncements) 정보를 외부로 응답하기 위한 DTO (Response 전용)
 * - Entity의 내부 로직이나 연관 관계를 숨기고, 사용자/프론트엔드에 필요한 핵심 데이터만 노출함
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
public class HousingAnnouncementsResponse {

    /** 공고 고유 식별자 (PK) */
    private Long id;

    /** 공고명 또는 주택명 */
    private String name;

    /** 제공 기관명 */
    private String provider;

    /** 모집 지역명 */
    private String regionName;

    /** 공고 게시일 */
    private LocalDate noticeDate;

    /** 공고 마감일 */
    private LocalDate closeDate;

    /** 공고 진행 상태 */
    private HousingStatus status;

    /** 공고 유형 */
    private HousingCategory category;

    /** 상세 페이지 URL */
    private String detailUrl;

    /** 추천 점수 (낮을수록 추천순위 높음) */
    private Double score;
    /** 추천 근거 요약 */
    private String reason;
}
