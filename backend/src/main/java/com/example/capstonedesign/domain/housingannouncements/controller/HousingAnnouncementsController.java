package com.example.capstonedesign.domain.housingannouncements.controller;

import com.example.capstonedesign.domain.housingannouncements.dto.response.HousingAnnouncementsResponse;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingStatus;
import com.example.capstonedesign.domain.housingannouncements.service.HousingAnnouncementsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * LH(한국토지주택공사) 주택 공고 관련 REST API Controller
 * - 클라이언트의 공고 조회, 검색, 추천 요청을 처리
 * - Service 계층 호출을 통해 실제 비즈니스 로직 수행
 * - 페이징, 정렬, 검색 필터, 사용자 맞춤 추천 등을 지원
 */
@Slf4j
@Tag(name = "Housing", description = "LH 주택 공고 조회 API")
@RestController
@RequestMapping("/api/housings")
@RequiredArgsConstructor
public class HousingAnnouncementsController {

    private final HousingAnnouncementsService service;

    /**
     * [공고 전체 조회]
     * -------------------------------------------------
     * LH에서 수집된 모든 주택 공고를 페이징 형태로 반환
     * <p>
     * 요청 예시:
     * GET /api/housings?page=0&size=10&sort=noticeDate,desc
     *
     * @param pageable 페이징 및 정렬 정보 (기본: noticeDate 내림차순)
     * @return 전체 공고 목록 (Page 형태)
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
     * [공고 조건 검색]
     * -------------------------------------------------
     * 주택 유형, 공고 상태, 키워드(제목/지역/카테고리 포함)를 기준으로 조건에 맞는 LH 주택 공고 검색
     * <p>
     * 요청 예시:
     * GET /api/housings/search?category=임대주택&status=공고중&keyword=서울
     *
     * @param category 주택 유형 (예: 임대주택, 분양주택)
     * @param status   공고 상태 (예: 공고중, 접수중, 종료)
     * @param keyword  검색 키워드 (제목, 지역명, 카테고리명)
     * @param pageable 페이징 및 정렬 정보 (기본: panNtStDt 내림차순)
     * @return 검색 결과 목록 (Page 형태)
     */
    @Operation(summary = "공고 조건 검색", description = "주택 유형, 상태, 키워드(제목/지역명 등)로 공고를 검색합니다.")
    @GetMapping("/search")
    public Page<HousingAnnouncementsResponse> search(
            @RequestParam(required = false) HousingCategory category,
            @RequestParam(required = false) HousingStatus status,
            @RequestParam(required = false, name = "region") String keyword,
            @ParameterObject
            @PageableDefault(sort = "panNtStDt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        // 빈 문자열 또는 공백만 입력된 경우 null 처리
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }
        return service.search(category, status, keyword, pageable);
    }

    /**
     * [마감 임박 공고 조회]
     * -------------------------------------------------
     * 오늘 기준으로 3일 이내에 마감되는 LH 공고 조회
     * <p>
     * 요청 예시:
     * GET /api/housings/closing-soon
     *
     * @param pageable 페이징 및 정렬 정보 (기본: closeDate 오름차순)
     * @return 마감 임박 공고 목록 (Page 형태)
     */
    @Operation(summary = "마감 임박 공고 조회", description = "오늘 기준 3일 이내 마감되는 공고를 조회합니다.")
    @GetMapping("/closing-soon")
    public Page<HousingAnnouncementsResponse> closingSoon(
            @ParameterObject
            @PageableDefault(sort = "closeDate", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return service.getClosingSoon(pageable);
    }

    /**
     * [최근 등록 공고 조회]
     * -------------------------------------------------
     * 최근 7일 이내 등록된 LH 공고를 최신순으로 조회
     * <p>
     * 요청 예시:
     * GET /api/housings/recent
     *
     * @param pageable 페이징 및 정렬 정보 (기본: noticeDate 내림차순)
     * @return 최근 등록 공고 목록 (Page 형태)
     */
    @Operation(summary = "최근 등록 공고 조회", description = "최근 7일 이내 등록된 LH 공고를 조회합니다.")
    @GetMapping("/recent")
    public Page<HousingAnnouncementsResponse> recent(
            @ParameterObject
            @PageableDefault(sort = "noticeDate", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return service.getRecent(pageable);
    }

    /**
     * [사용자 맞춤 공고 추천]
     * -------------------------------------------------
     * 사용자의 나이, 지역, 소득 구간(중위소득 100~300%)을 바탕으로 개인화된 LH 주택 공고 추천
     * <p>
     * 요청 예시:
     * GET /api/housings/recommend/{userId}?strictRegionMatch=false
     *
     * @param userId 사용자 ID
     * @param strictRegionMatch 지역 일치 여부 (true: 시·군·구만, false: 광역시·도 포함)
     * @return 맞춤형 LH 주택 공고 추천 목록 (상위 10개)
     */
    @Operation(summary = "사용자 맞춤 LH 주택 공고 추천", description = """
        사용자의 나이, 지역, 소득 구간(중위소득 100~300%)을 기반으로 맞춤형 LH 주택 공고를 추천합니다.
        - 청년층(20~35세) 및 저소득층은 우선 추천
        - 지역 일치도 및 마감 임박도 반영
        - 내부 산출 점수 기준 상위 10개 공고 반환
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
