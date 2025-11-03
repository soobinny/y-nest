package com.example.capstonedesign.domain.housingannouncements.entity;

/**
 * HousingStatus
 * -----------------------------------------------------
 * LH(한국토지주택공사) 공고 상태를 표준화한 Enum
 * - LH API의 panSs 값을 기준으로 통합 분류
 * - 공고 목록 및 추천 로직에서 상태 필터링에 사용
 */
public enum HousingStatus {

    /** 공고가 게시된 상태 (아직 접수 전) */
    공고중,

    /** 신청 접수가 진행 중인 상태 */
    접수중,

    /** 공고 내용이 정정되어 수정 공지가 올라온 상태 */
    정정공고중,

    /** 신청 기간이 마감된 상태 */
    접수마감,

    /** 최종적으로 모집이 완료되거나 종료된 상태 */
    모집완료,

    /** 공고가 취소되거나 삭제된 상태 */
    종료
}
