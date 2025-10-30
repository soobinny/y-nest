package com.example.capstonedesign.domain.youthpolicies.dto.response;

import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import lombok.Builder;
import lombok.Data;

/**
 * YouthPolicyResponse
 * -------------------------------------------------
 * - 청년정책 조회 응답 DTO
 * - 엔티티(YouthPolicy) → 응답용 데이터 변환 담당
 */
@Data
@Builder
public class YouthPolicyResponse {

    private String policyNo;        // 정책 번호
    private String policyName;      // 정책명
    private String categoryLarge;   // 대분류
    private String categoryMiddle;  // 중분류
    private String keyword;         // 키워드
    private String agency;          // 주관기관
    private String regionCode;      // 지역 코드
    private String startDate;       // 시작일
    private String endDate;         // 종료일
    private String supportContent;  // 지원 내용
    private String applyUrl;        // 신청 URL

    /** 엔티티 → DTO 변환 */
    public static YouthPolicyResponse fromEntity(YouthPolicy policy) {
        return YouthPolicyResponse.builder()
                .policyNo(policy.getPolicyNo())
                .policyName(policy.getPolicyName())
                .categoryLarge(policy.getCategoryLarge())
                .categoryMiddle(policy.getCategoryMiddle())
                .keyword(policy.getKeyword())
                .agency(policy.getAgency())
                .regionCode(policy.getRegionCode())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .supportContent(policy.getSupportContent())
                .applyUrl(policy.getApplyUrl())
                .build();
    }
}
