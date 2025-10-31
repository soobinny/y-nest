package com.example.capstonedesign.infra.finlife.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 금융감독원 오픈API: 주택담보대출 / 전세자금대출 / 개인신용대출 응답 DTO
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinlifeLoanProductResponse {

    @JsonProperty("result")
    private Result result;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("baseList")
        private List<Base> baseList;

        @JsonProperty("optionList")
        private List<Option> optionList;

        @JsonProperty("max_page_no")
        private Integer maxPageNo;
    }

    /**
     * 상품 기본 정보 목록
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Base {
        @JsonProperty("fin_co_no")
        private String finCoNo;       // 금융회사 코드

        @JsonProperty("fin_prdt_cd")
        private String finPrdtCd;     // 상품코드

        @JsonProperty("fin_prdt_nm")
        private String finPrdtNm;     // 상품명

        @JsonProperty("kor_co_nm")
        private String companyName;   // 회사명 (주의: kor_co_nm)

        @JsonProperty("dcls_url")
        private String detailUrl;     // 상세 URL

        @JsonProperty("join_way")
        private String joinWay;       // 가입/신청 방법

        @JsonProperty("join_member")
        private String joinMember;    // 대상

        @JsonProperty("etc_note")
        private String etcNote;       // 비고
    }

    /**
     * 상품 옵션 정보 (대출 금리 정보)
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Option {
        @JsonProperty("fin_co_no")
        private String finCoNo;         // 금융 회사 코드

        @JsonProperty("fin_prdt_cd")
        private String finPrdtCd;       // 상품 코드

        @JsonProperty("lend_rate_min")
        private BigDecimal lendRateMin; // 최저 금리

        @JsonProperty("lend_rate_max")
        private BigDecimal lendRateMax; // 최고 금리

        @JsonProperty("lend_rate_avg")
        private BigDecimal lendRateAvg; // 평균 금리

        @JsonProperty("rpay_type_nm")
        private String rpayTypeName;    // 상환 방식명

        @JsonProperty("lend_type_nm")
        private String lendTypeName;    // 금리 유형명(고정/변동)

        @JsonProperty("mrtg_type_nm")
        private String mrtgTypeName;    // 담보 유형명(주담대 전용)
    }
}
