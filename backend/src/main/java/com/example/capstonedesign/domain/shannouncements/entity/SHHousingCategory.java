package com.example.capstonedesign.domain.shannouncements.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SH공사 공고 유형")
public enum SHHousingCategory {

    /** 전세 또는 월세 형태의 임차용 주택 */
    주택임대,

    /** 매매(분양) 형태의 소유권 이전 대상 주택 */
    주택분양
}
