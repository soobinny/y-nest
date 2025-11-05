package com.example.capstonedesign.domain.notifications.dto;

import com.example.capstonedesign.domain.housingannouncements.entity.LhNotice;
import com.example.capstonedesign.domain.shannouncements.entity.ShAnnouncement;
import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap;
import java.util.Map;

/**
 * RecentNoticeDto
 * -------------------------------------------------
 * 홈 화면의 "최근 게시물" 통합 표시용 DTO
 * (주거 / 정책 데이터를 공통 형태로 변환)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentNoticeDto {
    private String type;              // 게시물 유형 ("주거", "정책")
    private String title;             // 제목
    private String region;            // 지역명
    private LocalDateTime createdAt;  // 생성일
    private String link;              // 상세 페이지 링크

    // LH 공고 → DTO 변환
    public static RecentNoticeDto fromLh(LhNotice entity) {
        LocalDateTime createdAt = null;

        try {
            // 날짜 문자열 (yyyy.MM.dd) 파싱
            if (entity.getPanNtStDt() != null && !entity.getPanNtStDt().isBlank()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                createdAt = LocalDateTime.of(
                        LocalDate.parse(entity.getPanNtStDt(), formatter),
                        java.time.LocalTime.MIDNIGHT
                );
            }
        } catch (DateTimeParseException e) {
            createdAt = LocalDateTime.now(); // 파싱 실패 시 현재 시각
        }

        return RecentNoticeDto.builder()
                .type("주거")
                .title(entity.getPanNm())
                .region(entity.getCnpCdNm())
                .createdAt(createdAt != null ? createdAt : LocalDateTime.now())
                .link("/housing")
                .build();
    }

    // 🏙SH 공고 → DTO 변환
    public static RecentNoticeDto fromSh(ShAnnouncement entity) {
        LocalDateTime createdAt;

        // 가능한 날짜 필드 순서대로 사용
        if (entity.getPostDate() != null) createdAt = entity.getPostDate().atStartOfDay();
        else if (entity.getCrawledAt() != null) createdAt = entity.getCrawledAt();
        else if (entity.getUpdatedAt() != null) createdAt = entity.getUpdatedAt();
        else createdAt = LocalDateTime.now();

        return RecentNoticeDto.builder()
                .type("주거")
                .title(entity.getTitle())
                .region(entity.getRegion())
                .createdAt(createdAt)
                .link("/housing")
                .build();
    }

    // 청년정책 → DTO 변환
    public static RecentNoticeDto fromPolicy(YouthPolicy entity) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate today = LocalDate.now();

        LocalDate start;
        LocalDate end;

        try {
            // 정책 시작/종료일 파싱
            start = LocalDate.parse(entity.getStartDate(), formatter);
            end = LocalDate.parse(entity.getEndDate(), formatter);
        } catch (Exception e) {
            return null; // 날짜 형식 오류 시 제외
        }

        // 종료된 정책 or 너무 먼 미래 정책 제외
        if (end.isBefore(today) || start.isAfter(today.plusMonths(1))) return null;

        // 지역코드 → 지역명 매핑
        Map<String, String> regionMap = Map.ofEntries(
                new AbstractMap.SimpleEntry<>("11", "서울특별시"),
                new AbstractMap.SimpleEntry<>("26", "부산광역시"),
                new AbstractMap.SimpleEntry<>("27", "대구광역시"),
                new AbstractMap.SimpleEntry<>("28", "인천광역시"),
                new AbstractMap.SimpleEntry<>("29", "광주광역시"),
                new AbstractMap.SimpleEntry<>("30", "대전광역시"),
                new AbstractMap.SimpleEntry<>("31", "울산광역시"),
                new AbstractMap.SimpleEntry<>("36", "세종특별자치시"),
                new AbstractMap.SimpleEntry<>("41", "경기도"),
                new AbstractMap.SimpleEntry<>("42", "강원특별자치도"),
                new AbstractMap.SimpleEntry<>("43", "충청북도"),
                new AbstractMap.SimpleEntry<>("44", "충청남도"),
                new AbstractMap.SimpleEntry<>("45", "전북특별자치도"),
                new AbstractMap.SimpleEntry<>("46", "전라남도"),
                new AbstractMap.SimpleEntry<>("47", "경상북도"),
                new AbstractMap.SimpleEntry<>("48", "경상남도"),
                new AbstractMap.SimpleEntry<>("49", "제주특별자치도")
        );

        String regionCode = entity.getRegionCode();
        String readableRegion = null;

        // "11110,11140,..." → 앞 2자리 기준으로 변환
        if (regionCode != null && !regionCode.isBlank()) {
            String prefix = regionCode.substring(0, 2);
            readableRegion = regionMap.getOrDefault(prefix, regionCode);
        }

        return RecentNoticeDto.builder()
                .type("정책")
                .title(entity.getPolicyName())
                .region(readableRegion)
                .createdAt(start.atStartOfDay())
                .link("/policy")
                .build();
    }
}
