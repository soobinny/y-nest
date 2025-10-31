package com.example.capstonedesign.domain.shannouncements.dto.response;

import com.example.capstonedesign.domain.shannouncements.entity.ShAnnouncement;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ShAnnouncementResponse
 * -----------------------------------------------------
 * - SH공사 공고 조회용 DTO
 * - Entity → Response 변환 전용
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
public class ShAnnouncementResponse {

    private Long id;                          // 공고 ID
    private String title;                     // 제목
    private String department;                // 담당 부서
    private LocalDate postDate;               // 게시일
    private Integer views;                    // 조회수
    private String recruitStatus;             // 진행 상태
    private String supplyType;                // 공급 유형
    private List<Map<String, String>> attachments; // 첨부파일 목록

    /** 추천 점수 (낮을수록 추천순위 높음) */
    private Double score;
    /** 추천 근거 요약 */
    private String reason;

    /** Entity → DTO 변환 */
    public static ShAnnouncementResponse fromEntity(ShAnnouncement e) {
        List<Map<String, String>> attachments = parseJson(e.getAttachments());

        // 첨부파일이 비어 있으면 null 처리
        if (attachments == null || attachments.isEmpty()) {
            attachments = null;
        }

        return ShAnnouncementResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .department(e.getDepartment())
                .postDate(e.getPostDate())
                .views(e.getViews())
                .recruitStatus(e.getRecruitStatus())
                .supplyType(e.getSupplyType())
                .attachments(attachments)
                .build();
    }

    /** JSON 문자열 → List<Map> 변환 */
    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> parseJson(String json) {
        try {
            return new ObjectMapper().readValue(json, List.class);
        } catch (Exception ex) {
            return List.of();
        }
    }
}
