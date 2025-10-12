package com.example.capstonedesign.infra.finlife.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 예금/적금 공통 스키마(간소화)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinlifeProductResponse {
    @JsonProperty("result")
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("baseList")
        private List<ProductBase> baseList;
        @JsonProperty("optionList")
        private List<ProductOption> optionList; // 금리/기간 옵션
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductBase {
        @JsonProperty("fin_prdt_cd")
        private String finPrdtCd;

        @JsonProperty("fin_prdt_nm")
        private String finPrdtNm;

        @JsonProperty("kor_co_nm")
        private String companyName;

        @JsonProperty("fin_co_no")
        private String finCoNo;

        @JsonProperty("join_way")
        private String joinWay;

        @JsonProperty("join_member")
        private String joinMember;

        @JsonProperty("etc_note")
        private String etcNote;

        @JsonProperty("dcls_url")
        private String detailUrl; // 상세 링크
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductOption {
        @JsonProperty("fin_prdt_cd")
        private String finPrdtCd;

        @JsonProperty("fin_co_no")
        private String finCoNo;

        @JsonProperty("save_trm")
        private Integer termMonth;

        @JsonProperty("intr_rate")
        private BigDecimal interestRate; // 기본 금리

        @JsonProperty("intr_rate2")
        private BigDecimal interestRateMax; // 우대 금리
    }
}
