package com.example.capstonedesign.domain.finance.financeproducts.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * FinlifeCreditLoanResponse
 * - 금융감독원(Finlife) API의 "개인신용대출" 응답 DTO
 * - 외부 JSON 응답을 내부 객체로 역직렬화하는 용도
 * - 등급별 신용대출 금리(crdtGrad1~13) 포함
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinlifeCreditLoanResponse {

    /** API 최상위 result 블록 */
    @JsonProperty("result")
    private Result result;

    /** result 내부 구조: 상품 기본정보(baseList) + 금리옵션(optionList) */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("baseList")  private List<Base> baseList;   // 상품 기본 정보
        @JsonProperty("optionList") private List<Option> optionList; // 등급별 금리 옵션
    }

    /** 기본 상품 정보 (회사명, 상품명 등) */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Base {
        @JsonProperty("fin_co_no")   private String finCoNo;     // 금융회사 코드
        @JsonProperty("kor_co_nm")   private String korCoNm;     // 금융회사명
        @JsonProperty("fin_prdt_cd") private String finPrdtCd;   // 금융상품 코드
        @JsonProperty("fin_prdt_nm") private String finPrdtNm;   // 상품명
    }

    /** 신용대출 금리 옵션 정보 (등급별 금리 포함) */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Option {
        @JsonProperty("fin_co_no")   private String finCoNo;       // 금융회사 코드
        @JsonProperty("fin_prdt_cd") private String finPrdtCd;     // 금융상품 코드

        @JsonProperty("rpay_type_nm")      private String rpayTypeNm;      // 상환방식
        @JsonProperty("lend_rate_type_nm") private String lendRateTypeNm;  // 금리유형
        @JsonProperty("lend_rate_min")     private BigDecimal lendRateMin; // 최소금리
        @JsonProperty("lend_rate_max")     private BigDecimal lendRateMax; // 최대금리
        @JsonProperty("lend_rate_avg")     private BigDecimal lendRateAvg; // 평균금리

        // 신용등급별 금리 정보
        @JsonProperty("crdt_lend_rate_type")    private String crdtLendRateType;    // 금리유형 코드
        @JsonProperty("crdt_lend_rate_type_nm") private String crdtLendRateTypeNm;  // 금리유형명
        @JsonProperty("crdt_grad_1")  private BigDecimal crdtGrad1;   // 1등급 금리
        @JsonProperty("crdt_grad_4")  private BigDecimal crdtGrad4;   // 4등급 금리
        @JsonProperty("crdt_grad_5")  private BigDecimal crdtGrad5;   // 5등급 금리
        @JsonProperty("crdt_grad_6")  private BigDecimal crdtGrad6;   // 6등급 금리
        @JsonProperty("crdt_grad_10") private BigDecimal crdtGrad10;  // 10등급 금리
        @JsonProperty("crdt_grad_11") private BigDecimal crdtGrad11;  // 11등급 금리
        @JsonProperty("crdt_grad_12") private BigDecimal crdtGrad12;  // 12등급 금리
        @JsonProperty("crdt_grad_13") private BigDecimal crdtGrad13;  // 13등급 금리
        @JsonProperty("crdt_grad_avg") private BigDecimal crdtGradAvg; // 평균 금리
    }
}
