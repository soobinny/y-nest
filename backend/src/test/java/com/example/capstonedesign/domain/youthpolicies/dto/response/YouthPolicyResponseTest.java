package com.example.capstonedesign.domain.youthpolicies.dto.response;

import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YouthPolicyResponseTest {

    @Test
    @DisplayName("normalizeContent - 다양한 bullet 기호를 '-' 로 통일")
    void normalizeContent_unifyBullets() {
        // given
        String raw = """
                ○ 지원대상 : 만 19~34세 청년
                ● 지원내용 : 월세 지원
                * 비고 : 기타 사항
                """;

        Products product = mock(Products.class);
        when(product.getId()).thenReturn(1);

        YouthPolicy policy = YouthPolicy.builder()
                .id(1L)
                .product(product)
                .policyNo("P-1")
                .policyName("테스트 정책")
                .supportContent(raw)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when
        YouthPolicyResponse resp = YouthPolicyResponse.fromEntity(policy);

        // then
        assertThat(resp.getSupportContent()).contains("- 지원대상");
        assertThat(resp.getSupportContent()).contains("- 지원내용");
        assertThat(resp.getSupportContent()).contains("- 비고");
    }

    @Test
    @DisplayName("fromEntityWithRecommendation - score와 reason이 DTO에 매핑된다")
    void fromEntityWithRecommendation_mapsScoreAndReason() {
        // given
        Products product = mock(Products.class);
        when(product.getId()).thenReturn(1);

        YouthPolicy policy = YouthPolicy.builder()
                .id(1L)
                .product(product)
                .policyNo("P-1")
                .policyName("테스트 정책")
                .supportContent("내용")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        double score = 0.75;
        String reason = "나이 조건 적합, 저소득층 우대";

        // when
        YouthPolicyResponse resp = YouthPolicyResponse.fromEntityWithRecommendation(policy, score, reason);

        // then
        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getProductId()).isEqualTo(1);
        assertThat(resp.getScore()).isEqualTo(0.75);
        assertThat(resp.getReason()).isEqualTo(reason);
    }
}
