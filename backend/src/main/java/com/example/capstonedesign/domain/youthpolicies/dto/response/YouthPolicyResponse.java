package com.example.capstonedesign.domain.youthpolicies.dto.response;

import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * YouthPolicyResponse
 * -------------------------------------------------
 * - 청년정책 조회 응답 DTO
 * - 엔티티(YouthPolicy) → 응답용 데이터 변환 담당
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
public class YouthPolicyResponse {

    private Long id;                // 엔티티 PK (정책 상세조회 용)
    private Integer productId;      // 즐겨찾기/공통 product 매핑용

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

    /** 추천 점수 (낮을수록 추천순위 높음) */
    private Double score;
    /** 추천 근거 요약 */
    private String reason;

    /** 정책 설명 레이아웃 통일 처리 */
    private static String normalizeContent(String raw) {
        if (raw == null || raw.isBlank()) return "";

        String text = raw;

        // 1) 모든 bullet 기호를 '-' 로 통일
        text = text.replace("○", "-")
                .replace("◦", "-")
                .replace("·", "-")
                .replace("●", "-")
                .replace("◉", "-")
                .replace("◎", "-")
                .replace("◆", "-")
                .replace("■", "-")
                .replace("□", "-")
                .replace("▪", "-")
                .replace("▣", "-")
                .replace("•", "-")
                .replace("*", "-")
                .replace("❍", "-")
                .replace("ㅇ", "-");

        // 2) '-일시' → '- 일시' 같은 잘못된 패턴 보정
        text = text.replaceAll("-\\s*(일시|기간|장소|대상|목적|내용|주요내용)", "- $1");

        // 3) ': ' 없이 붙은 경우 정리
        text = text.replaceAll("\\s*:\\s*", " : ");

        // 4) 여러 줄바꿈 → 하나로 정리
        text = text.replaceAll("\n{2,}", "\n");

        // 5) 앞뒤 공백 제거
        text = text.trim();

        return text;
    }

    /** 엔티티 → DTO 변환 */
    public static YouthPolicyResponse fromEntity(YouthPolicy policy) {
        return YouthPolicyResponse.builder()
                .id(policy.getId())
                .productId(policy.getProduct().getId())
                .policyNo(policy.getPolicyNo())
                .policyName(policy.getPolicyName())
                .categoryLarge(policy.getCategoryLarge())
                .categoryMiddle(policy.getCategoryMiddle())
                .keyword(policy.getKeyword())
                .agency(policy.getAgency())
                .regionCode(policy.getRegionCode())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .supportContent(normalizeContent(policy.getSupportContent()))
                .applyUrl(policy.getApplyUrl())
                .build();
    }

    /** 추천 결과 포함 변환: 엔티티 + 점수 + 사유 */
    public static YouthPolicyResponse fromEntityWithRecommendation(
            YouthPolicy policy, double score, String reason) {

        return YouthPolicyResponse.builder()
                .id(policy.getId())
                .productId(policy.getProduct().getId())
                .policyNo(policy.getPolicyNo())
                .policyName(policy.getPolicyName())
                .categoryLarge(policy.getCategoryLarge())
                .categoryMiddle(policy.getCategoryMiddle())
                .keyword(policy.getKeyword())
                .agency(policy.getAgency())
                .regionCode(policy.getRegionCode())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .supportContent(normalizeContent(policy.getSupportContent()))
                .applyUrl(policy.getApplyUrl())
                .score(score)
                .reason(reason)
                .build();
    }
}