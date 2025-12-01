package com.example.capstonedesign.domain.finance.financeproducts.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * FinlifeMortgageLoanResponse
 * - 금융감독원(Finlife) API의 "주택담보대출" 응답 DTO
 * - 외부 API JSON을 내부 객체로 매핑하기 위한 구조
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinlifeMortgageLoanResponse {

    /** API 응답 전체 result 블록 */
    @JsonProperty("result")
    private Result result;

    /** result 내부 구조: 기본정보(baseList) + 옵션정보(optionList) */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private List<Base> baseList;     // 상품 기본 정보
        private List<Option> optionList; // 금리/담보/상환 옵션
    }

    /** 기본 상품 정보 (금융사명, 상품명 등) */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Base {
        @JsonProperty("fin_prdt_cd") private String finPrdtCd;  // 금융상품 코드
        @JsonProperty("fin_co_no")   private String finCoNo;    // 금융회사 코드
        @JsonProperty("kor_co_nm")   private String korCoNm;    // 금융회사명
        @JsonProperty("fin_prdt_nm") private String finPrdtNm;  // 상품명
        @JsonProperty("loan_lmt")    private String loanLmt;    // 대출한도 (문자열 형태)
    }

    /** 금리 및 옵션 정보 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Option {
        @JsonProperty("fin_prdt_cd") private String finPrdtCd;     // 금융상품 코드
        @JsonProperty("fin_co_no")   private String finCoNo;       // 금융회사 코드

        @JsonProperty("mrtg_type_nm")   private String mrtgTypeNm; // 담보유형 (예: 아파트, 주택)
        @JsonProperty("rpay_type_nm")   private String rpayTypeNm; // 상환방식 (예: 원리금균등, 만기일시)
        @JsonProperty("lend_rate_type_nm") private String lendRateTypeNm; // 금리유형 (고정/변동)

        @JsonProperty("lend_rate_min") private BigDecimal lendRateMin; // 최소금리
        @JsonProperty("lend_rate_max") private BigDecimal lendRateMax; // 최대금리
        @JsonProperty("lend_rate_avg") private BigDecimal lendRateAvg; // 평균금리
    }
}
