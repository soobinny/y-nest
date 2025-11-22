package com.example.capstonedesign.domain.housingannouncements.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HousingAnnouncementsResponseTest
 * ---------------------------------------------
 * - 주거 공고 응답 DTO의 빌더/게터 동작 확인
 * - 현재 별도의 fromEntity 로직은 없으므로
 *   DTO 필드 매핑이 의도대로 되는지만 검증
 */
class HousingAnnouncementsResponseTest {

    @Test
    @DisplayName("HousingAnnouncementsResponse - 빌더로 세팅한 값들이 그대로 매핑된다")
    void housingAnnouncementsResponse_builder_shouldSetFieldsCorrectly() {
        // given
        LocalDate noticeDate = LocalDate.of(2025, 1, 10);
        LocalDate closeDate = LocalDate.of(2025, 1, 31);

        HousingAnnouncementsResponse dto = HousingAnnouncementsResponse.builder()
                .productId(10)
                .id(100L)
                .name("서울 청년 전세주택 A단지 모집공고")
                .provider("LH공사")
                .regionName("서울특별시 강남구")
                .noticeDate(noticeDate)
                .closeDate(closeDate)
                .status(null)      // 현재 테스트에서는 enum 값까지는 강제하지 않음
                .category(null)
                .detailUrl("https://example.com/housings/100")
                .score(12.5)
                .reason("청년 + 무주택 + 서울 지역 우선 추천")
                .build();

        // then
        assertThat(dto.getProductId()).isEqualTo(10);
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getName()).isEqualTo("서울 청년 전세주택 A단지 모집공고");
        assertThat(dto.getProvider()).isEqualTo("LH공사");
        assertThat(dto.getRegionName()).isEqualTo("서울특별시 강남구");
        assertThat(dto.getNoticeDate()).isEqualTo(noticeDate);
        assertThat(dto.getCloseDate()).isEqualTo(closeDate);
        assertThat(dto.getDetailUrl()).isEqualTo("https://example.com/housings/100");
        assertThat(dto.getScore()).isEqualTo(12.5);
        assertThat(dto.getReason()).contains("청년");
    }
}
