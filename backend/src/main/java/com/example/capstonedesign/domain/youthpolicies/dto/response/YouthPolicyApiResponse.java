package com.example.capstonedesign.domain.youthpolicies.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * YouthPolicyApiResponse
 * -------------------------------------------------
 * - 온통청년(Youth Center) 정책 API 응답 DTO
 * - 외부 JSON 응답을 자바 객체로 매핑
 * - result → pagging 정보 및 정책 목록 포함
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouthPolicyApiResponse {

    @JsonProperty("resultCode")
    private int resultCode;            // 응답 코드

    @JsonProperty("resultMessage")
    private String resultMessage;      // 응답 메시지

    @JsonProperty("result")
    private Result result;             // 결과 데이터

    /** result 객체 내부 구조 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {

        @JsonProperty("pagging")
        private Pagging pagging;       // 페이징 정보

        @JsonProperty("youthPolicyList")
        private List<PolicyItem> youthPolicyList;  // 정책 리스트
    }

    /** 페이징 정보 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagging {
        @JsonProperty("totCount")
        private int totCount;          // 전체 건수
        @JsonProperty("pageNum")
        private int pageNum;           // 현재 페이지
        @JsonProperty("pageSize")
        private int pageSize;          // 페이지 크기
    }

    /** 정책 단건 정보 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PolicyItem {
        private String plcyNo;            // 정책 번호
        private String plcyNm;            // 정책명
        private String plcyKywdNm;        // 키워드명
        private String plcyExplnCn;       // 정책 설명
        private String lclsfNm;           // 대분류명
        private String mclsfNm;           // 중분류명
        private String sprvsnInstCdNm;    // 주관기관명
        private String aplyUrlAddr;       // 신청 URL
        private String zipCd;             // 지역 코드
        private String sprtTrgtMinAge;    // 최소 연령
        private String sprtTrgtMaxAge;    // 최대 연령
        private String plcySprtCn;        // 지원 내용
        private String bizPrdBgngYmd;     // 사업 시작일
        private String bizPrdEndYmd;      // 사업 종료일
    }

    /** 빈 응답용 정적 메서드 */
    public static YouthPolicyApiResponse empty() {
        YouthPolicyApiResponse resp = new YouthPolicyApiResponse();

        resp.setResultCode(0);  // 기본값: 0 (정상), 혹은 -1로 지정 가능
        resp.setResultMessage("NO_DATA");

        Result result = new Result();
        result.setYouthPolicyList(Collections.emptyList());
        resp.setResult(result);

        return resp;
    }
}
