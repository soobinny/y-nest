package com.example.capstonedesign.domain.housingannouncements.entity;

/**
 * HousingCategory
 * -----------------------------------------------------
 * LH 공고의 상위 분류(uppAisTpNm)를 기준으로
 * 실제 유형을 세분화하여 정의
 */
public enum HousingCategory {

    /** 임대주택 (국민임대, 행복주택, 공공임대 등) */
    임대주택,

    /** 분양주택 (공공분양, 신혼희망타운 등) */
    분양주택,

    /** 상가 (임대상가, 희망상가 등) */
    상가,

    /** 토지 (산업시설용지, 주상복합용지 등) */
    토지,

    /** 주거복지 (매입임대, 전세임대 등) */
    주거복지,

    /** 기타 (정의되지 않은 유형) */
    기타
}
