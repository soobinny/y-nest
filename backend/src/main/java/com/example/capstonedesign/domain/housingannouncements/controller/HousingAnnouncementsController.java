package com.example.capstonedesign.domain.housingannouncements.controller;

import com.example.capstonedesign.domain.housingannouncements.dto.response.HousingAnnouncementsResponse;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingStatus;
import com.example.capstonedesign.domain.housingannouncements.service.HousingAnnouncementsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HousingAnnouncementsController
 * -----------------------------------------------------
 * - LH 주택 공고 관련 조회 API의 진입점(Controller)
 * - 클라이언트 요청을 받아 Service 계층으로 전달
 * - 페이징, 정렬, 검색 필터 등의 요청 파라미터를 처리
 */
@Tag(name = "Housing", description = "LH 주택 공고 조회 API")
@RestController
@RequestMapping("/api/housings")
@RequiredArgsConstructor
public class HousingAnnouncementsController {

    private final HousingAnnouncementsService service;

    /**
     * 전체 LH 주택 공고 조회 API
     * -------------------------------------------------
     * 요청 예시:
     * GET /api/housings?page=0&size=10&sort=noticeDate,desc
     *
     * @param pageable 페이징 및 정렬 정보 (기본: noticeDate 내림차순)
     * @return Page<HousingAnnouncementsResponse>
     */
    @Operation(summary = "공고 전체 조회 (페이징)")
    @GetMapping
    public Page<HousingAnnouncementsResponse> getAll(
            @ParameterObject
            @PageableDefault(sort = "noticeDate", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return service.getAll(pageable);
    }

    /**
     * 공고 조건 검색 API
     * -------------------------------------------------
     * 요청 예시:
     * GET /api/housings/search?category=임대주택&status=공고중&region=서울
     *
     * @param category 주택 유형 (예: 임대주택, 분양주택)
     * @param status   공고 상태 (예: 공고중, 마감)
     * @param region   지역명 (부분 검색 가능)
     * @param pageable 페이징 및 정렬 정보 (기본: noticeDate 내림차순)
     * @return 조건에 맞는 공고 목록 페이지
     */
    @Operation(summary = "공고 상세 검색 (카테고리/상태/지역)")
    @GetMapping("/search")
    public Page<HousingAnnouncementsResponse> search(
            @RequestParam(required = false) HousingCategory category,
            @RequestParam(required = false) HousingStatus status,
            @RequestParam(defaultValue = "") String region,
            @ParameterObject
            @PageableDefault(sort = "noticeDate", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return service.search(category, status, region, pageable);
    }

    /**
     * 마감 임박 공고 조회 API
     * -------------------------------------------------
     * 오늘 기준으로 3일 이내 마감되는 공고를 조회
     * GET /api/housings/closing-soon
     *
     * @param pageable 페이징 및 정렬 정보 (기본: closeDate 오름차순)
     * @return 마감 임박 공고 목록 페이지
     */
    @Operation(summary = "마감 임박 공고 조회 (3일 내 마감)")
    @GetMapping("/closing-soon")
    public Page<HousingAnnouncementsResponse> closingSoon(
            @ParameterObject
            @PageableDefault(sort = "closeDate", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return service.getClosingSoon(pageable);
    }

    /**
     * 최근 등록 공고 조회 API
     * -------------------------------------------------
     * 최근 7일 이내 등록된 공고만 조회
     * GET /api/housings/recent
     *
     * @param pageable 페이징 및 정렬 정보 (기본: noticeDate 내림차순)
     * @return 최근 등록 공고 목록 페이지
     */
    @Operation(summary = "최근 공고 조회 (7일 이내 등록)")
    @GetMapping("/recent")
    public Page<HousingAnnouncementsResponse> recent(
            @ParameterObject
            @PageableDefault(sort = "noticeDate", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return service.getRecent(pageable);
    }
}
