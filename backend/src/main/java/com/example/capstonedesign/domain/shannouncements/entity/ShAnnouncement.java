package com.example.capstonedesign.domain.shannouncements.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SH공사 공고 엔티티
 * - 임대/분양 등 주택 관련 공고 데이터 저장
 * - i-SH 사이트에서 크롤링된 정보 기준
 */
@Entity
@Table(name = "sh_announcements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source", "external_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK

    private String source;        // 출처 (예: "i-sh")
    private String externalId;    // 외부 고유 ID (seq 값)
    private String title;         // 공고 제목
    private String department;    // 담당 부서
    private LocalDate postDate;   // 게시일
    private Integer views;        // 조회수

    private String recruitStatus; // 진행 상태 (now: 진행중 / suc: 완료)
    private String supplyType;    // 공급유형 (행복주택, 청년안심주택 등)
    private String category;      // 카테고리 (임대 / 분양)

    @Lob
    private String contentHtml;   // 본문 HTML

    @Column(columnDefinition = "json")
    private String attachments;   // 첨부파일 목록(JSON)

    @Column(length = 50)
    private String region;        // 지역명 (서울, 강남, 송파 등)

    private LocalDateTime crawledAt; // 크롤링 시각
    private LocalDateTime updatedAt; // 업데이트 시각
}