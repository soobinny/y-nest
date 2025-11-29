package com.example.capstonedesign.domain.notifications.dto;

import com.example.capstonedesign.domain.housingannouncements.entity.LhNotice;
import com.example.capstonedesign.domain.shannouncements.entity.ShAnnouncement;
import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RecentNoticeDtoTest {

    // ---------------------------------------------------------------------
    // LH 공고 → DTO 변환 테스트
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("fromLh() - 유효한 날짜 문자열이면 파싱된 LocalDateTime을 사용한다")
    void fromLh_validDate_usesParsedLocalDateTime() {
        // given
        LhNotice entity = new LhNotice();
        entity.setPanNtStDt("2024.11.01");
        entity.setPanNm("테스트 LH 공고");
        entity.setCnpCdNm("서울특별시");
        entity.setDtlUrl("https://example.com/lh/1");

        // when
        RecentNoticeDto dto = RecentNoticeDto.fromLh(entity);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getType()).isEqualTo("주거");
        assertThat(dto.getTitle()).isEqualTo("테스트 LH 공고");
        assertThat(dto.getRegion()).isEqualTo("서울특별시");
        assertThat(dto.getLink()).isEqualTo("https://example.com/lh/1");

        // createdAt은 2024-11-01T00:00:00 이어야 함
        assertThat(dto.getCreatedAt())
                .isEqualTo(LocalDateTime.of(2024, 11, 1, 0, 0));
    }

    @Test
    @DisplayName("fromLh() - 날짜 파싱 실패 시 createdAt은 now()로 대체된다")
    void fromLh_invalidDate_fallsBackToNow() {
        // given
        LhNotice entity = new LhNotice();
        entity.setPanNtStDt("invalid-date");
        entity.setPanNm("파싱 실패 LH 공고");
        entity.setCnpCdNm("부산광역시");
        entity.setDtlUrl("https://example.com/lh/2");

        // when
        LocalDateTime before = LocalDateTime.now();
        RecentNoticeDto dto = RecentNoticeDto.fromLh(entity);
        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(dto).isNotNull();
        // now()로 대체되기 때문에 before~after 사이에 있는지만 체크
        assertThat(dto.getCreatedAt()).isBetween(before, after);
    }

    // ---------------------------------------------------------------------
    // SH 공고 → DTO 변환 테스트
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("fromSh() - postDate가 존재하면 postDate 기준으로 createdAt을 설정한다")
    void fromSh_usePostDateWhenAvailable() {
        // given
        ShAnnouncement entity = new ShAnnouncement();
        entity.setPostDate(LocalDate.of(2024, 10, 10));
        entity.setCrawledAt(null);
        entity.setUpdatedAt(null);
        entity.setTitle("SH 공고 제목");
        entity.setRegion("서울특별시 강남구");
        entity.setDetailUrl("https://example.com/sh/1");

        // when
        RecentNoticeDto dto = RecentNoticeDto.fromSh(entity);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getType()).isEqualTo("주거");
        assertThat(dto.getTitle()).isEqualTo("SH 공고 제목");
        assertThat(dto.getRegion()).isEqualTo("서울특별시 강남구");
        assertThat(dto.getLink()).isEqualTo("https://example.com/sh/1");
        assertThat(dto.getCreatedAt())
                .isEqualTo(LocalDate.of(2024, 10, 10).atStartOfDay());
    }

    @Test
    @DisplayName("fromSh() - postDate 없으면 crawledAt을, 그것도 없으면 updatedAt을 사용한다")
    void fromSh_fallbackToCrawledAtOrUpdatedAt() {
        // given
        LocalDateTime crawledAt = LocalDateTime.of(2024, 9, 1, 12, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 8, 1, 9, 0);

        ShAnnouncement entity = new ShAnnouncement();
        entity.setPostDate(null);
        entity.setCrawledAt(crawledAt);
        entity.setUpdatedAt(updatedAt);
        entity.setTitle("SH 공고 크롤링");
        entity.setRegion("부산광역시");
        entity.setDetailUrl("https://example.com/sh/2");

        // when
        RecentNoticeDto dto = RecentNoticeDto.fromSh(entity);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getCreatedAt()).isEqualTo(crawledAt);
    }

    // ---------------------------------------------------------------------
    // 청년정책 → DTO 변환 테스트
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("fromPolicy() - 유효 기간 내 정책이면 DTO를 반환하고 지역 코드 매핑을 수행한다")
    void fromPolicy_validWithinOneMonth_returnsDto() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate start = today;
        LocalDate end = today.plusDays(10);

        YouthPolicy entity = new YouthPolicy();
        var formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");

        entity.setStartDate(start.format(formatter));
        entity.setEndDate(end.format(formatter));
        entity.setRegionCode("11110"); // 서울특별시 코드(앞 2자리 11)
        entity.setPolicyName("청년 주거 지원 정책");
        entity.setApplyUrl("https://example.com/policy/1");

        // when
        RecentNoticeDto dto = RecentNoticeDto.fromPolicy(entity);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getType()).isEqualTo("정책");
        assertThat(dto.getTitle()).isEqualTo("청년 주거 지원 정책");
        assertThat(dto.getRegion()).isEqualTo("서울특별시");
        assertThat(dto.getLink()).isEqualTo("https://example.com/policy/1");
        assertThat(dto.getCreatedAt()).isEqualTo(start.atStartOfDay());
    }

    @Test
    @DisplayName("fromPolicy() - 이미 종료된 정책이면 null을 반환한다")
    void fromPolicy_expiredPolicy_returnsNull() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusMonths(2);
        LocalDate end = today.minusDays(1);

        YouthPolicy entity = new YouthPolicy();
        var formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");

        entity.setStartDate(start.format(formatter));
        entity.setEndDate(end.format(formatter));
        entity.setRegionCode("11110");
        entity.setPolicyName("지난 정책");
        entity.setApplyUrl("https://example.com/policy/expired");

        // when
        RecentNoticeDto dto = RecentNoticeDto.fromPolicy(entity);

        // then
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("fromPolicy() - 시작일이 한 달 이후인 먼 미래 정책이면 null을 반환한다")
    void fromPolicy_tooFarFuturePolicy_returnsNull() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate start = today.plusMonths(2);
        LocalDate end = start.plusDays(5);

        YouthPolicy entity = new YouthPolicy();
        var formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");

        entity.setStartDate(start.format(formatter));
        entity.setEndDate(end.format(formatter));
        entity.setRegionCode("11110");
        entity.setPolicyName("먼 미래 정책");
        entity.setApplyUrl("https://example.com/policy/future");

        // when
        RecentNoticeDto dto = RecentNoticeDto.fromPolicy(entity);

        // then
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("fromPolicy() - 날짜 형식이 잘못되면 null을 반환한다")
    void fromPolicy_invalidDateFormat_returnsNull() {
        // given
        YouthPolicy entity = new YouthPolicy();
        entity.setStartDate("2024-01-01"); // 잘못된 형식
        entity.setEndDate("2024-12-31");
        entity.setRegionCode("11110");
        entity.setPolicyName("형식 오류 정책");
        entity.setApplyUrl("https://example.com/policy/invalid");

        // when
        RecentNoticeDto dto = RecentNoticeDto.fromPolicy(entity);

        // then
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("fromPolicy() - 매핑되지 않는 지역코드는 원본 문자열을 지역으로 사용한다")
    void fromPolicy_unknownRegionCode_usesOriginalCode() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate start = today;
        LocalDate end = today.plusDays(3);

        YouthPolicy entity = new YouthPolicy();
        var formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");

        entity.setStartDate(start.format(formatter));
        entity.setEndDate(end.format(formatter));
        entity.setRegionCode("99999"); // regionMap에 없는 코드
        entity.setPolicyName("테스트 정책");
        entity.setApplyUrl("https://example.com/policy/unknown-region");

        // when
        RecentNoticeDto dto = RecentNoticeDto.fromPolicy(entity);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getRegion()).isEqualTo("99999");
    }
}
