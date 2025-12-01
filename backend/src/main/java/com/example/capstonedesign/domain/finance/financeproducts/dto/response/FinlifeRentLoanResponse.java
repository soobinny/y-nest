package com.example.capstonedesign.domain.finance.financeproducts.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * FinlifeRentLoanResponse
 * - 금융감독원(Finlife) API의 "전세자금대출" 응답 DTO
 * - 외부 API JSON을 내부 객체로 매핑하기 위한 구조
 * - 주택담보대출과 달리 담보유형(mrtgTypeNm)은 없음
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinlifeRentLoanResponse {

    /** API 응답의 result 블록 */
    @JsonProperty("result")
    private Result result;

    /** result 내부 구조: 상품 기본정보(baseList) + 금리옵션(optionList) */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private List<Base> baseList;     // 기본 상품 정보
        private List<Option> optionList; // 금리 및 상환 방식 정보
    }

    /** 기본 상품 정보 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Base {
        @JsonProperty("fin_prdt_cd") private String finPrdtCd;  // 금융상품 코드
        @JsonProperty("fin_co_no")   private String finCoNo;    // 금융회사 코드
        @JsonProperty("kor_co_nm")   private String korCoNm;    // 금융회사명
        @JsonProperty("fin_prdt_nm") private String finPrdtNm;  // 상품명
        @JsonProperty("loan_lmt")    private String loanLmt;    // 대출한도 (문자열)
    }

    /** 금리 및 옵션 정보 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Option {
        @JsonProperty("fin_prdt_cd") private String finPrdtCd;     // 금융상품 코드
        @JsonProperty("fin_co_no")   private String finCoNo;       // 금융회사 코드

        @JsonProperty("rpay_type_nm")      private String rpayTypeNm;       // 상환방식 (예: 원리금균등, 만기일시)
        @JsonProperty("lend_rate_type_nm") private String lendRateTypeNm;   // 금리유형 (예: 고정금리, 변동금리)
        @JsonProperty("lend_rate_min")     private BigDecimal lendRateMin;  // 최소금리
        @JsonProperty("lend_rate_max")     private BigDecimal lendRateMax;  // 최대금리
        @JsonProperty("lend_rate_avg")     private BigDecimal lendRateAvg;  // 평균금리
    }
}
