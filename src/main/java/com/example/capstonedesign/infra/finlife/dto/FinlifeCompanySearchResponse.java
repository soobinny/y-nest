package com.example.capstonedesign.infra.finlife.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinlifeCompanySearchResponse {
    @JsonProperty("result")
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("baseList")
        private List<Company> baseList;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Company {
        @JsonProperty("fin_co_no")
        private String finCoNo;
        @JsonProperty("kor_co_nm")
        private String name;
        @JsonProperty("homp_url")
        private String homepage;
        @JsonProperty("cal_tel")
        private String contact;
    }
}
