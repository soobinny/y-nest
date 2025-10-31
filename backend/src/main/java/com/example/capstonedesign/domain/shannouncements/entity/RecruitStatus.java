package com.example.capstonedesign.domain.shannouncements.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * SH공사 공고 모집 상태
 * - now: 진행 중
 * - suc: 완료
 */
@Schema(description = "모집 상태 (진행 중/완료)")
public enum RecruitStatus {
    now,  // 진행 중
    suc;  // 완료
}