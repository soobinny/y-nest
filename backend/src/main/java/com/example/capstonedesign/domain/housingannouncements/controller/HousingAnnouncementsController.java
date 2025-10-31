package com.example.capstonedesign.domain.housingannouncements.controller;

import com.example.capstonedesign.domain.housingannouncements.dto.response.HousingAnnouncementsResponse;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingStatus;
import com.example.capstonedesign.domain.housingannouncements.service.HousingAnnouncementsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * 사용자 맞춤 LH 주택 공고 추천 API
     * -------------------------------------------------
     * 사용자의 나이, 지역, 소득 구간(중위소득 100~300%) 정보를 기반으로 개인 맞춤형 LH 주택 공고 추천
     * GET /api/housings/recommend/{userId}?strictRegionMatch=false
     *
     * @param userId 사용자 ID
     * @param strictRegionMatch 지역 일치 여부 (true=시·군·구만, false=광역시·도 포함)
     * @return 맞춤형 추천 LH 주택 공고 목록
     */
    @Operation(summary = "사용자 맞춤 LH 주택 공고 추천", description = """
    사용자의 나이, 지역, 소득 구간(중위소득 100~300%)을 기준으로 맞춤형 LH 주택 공고를 추천합니다.

    추천 로직 요약:
    - 청년층(20~35세) 및 저소득층(중위소득 150% 이하): 소액 예치가 가능하고 금리가 높은 상품 우대
    - 중위·고소득층(200~300%): 예치금 규모가 크고 안정성이 높은 상품(정기예금 중심) 추천
    - 금리가 높을수록, 최소 예치금이 낮을수록, 점수가 낮을수록 상위 노출
    - 내부적으로 산출된 종합 점수에 따라 상위 10개의 추천 상품 반환
    """)
    @GetMapping("/recommend/{userId}")
    public List<HousingAnnouncementsResponse> recommendForUser(
            @Parameter(description = "사용자 ID") @PathVariable Integer userId,
            @Parameter(description = "지역 일치 여부 (true=시·군·구만, false=광역시·도 포함)")
            @RequestParam(defaultValue = "false") boolean strictRegionMatch
    ) {
        return service.recommendForUser(userId, strictRegionMatch);
    }
}
