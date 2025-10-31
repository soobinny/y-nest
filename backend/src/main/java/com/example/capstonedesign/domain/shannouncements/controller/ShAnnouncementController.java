package com.example.capstonedesign.domain.shannouncements.controller;

import com.example.capstonedesign.domain.shannouncements.dto.response.ShAnnouncementResponse;
import com.example.capstonedesign.domain.shannouncements.entity.RecruitStatus;
import com.example.capstonedesign.domain.shannouncements.entity.SHHousingCategory;
import com.example.capstonedesign.domain.shannouncements.service.ShAnnouncementService;
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
 * ShAnnouncementController
 * -----------------------------------------------------
 * - SH공사(서울주택도시공사) 청년주거 공고 조회 API
 * - LH 공고 API와 동일한 구조로 설계 (페이징, 검색, 추천 등)
 */
@Tag(name = "SH Housing", description = "서울주택도시공사(SH) 청년주거 공고 조회 API")
@RestController
@RequestMapping("/api/sh/housings")
@RequiredArgsConstructor
public class ShAnnouncementController {

    private final ShAnnouncementService service;

    /** 전체 공고 조회 */
    @Operation(summary = "SH 공고 전체 조회 (페이징)")
    @GetMapping
    public Page<ShAnnouncementResponse> getAll(
            @ParameterObject
            @PageableDefault(sort = "postDate", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return service.getAll(pageable);
    }

    /** 조건 검색 (유형 / 상태 / 키워드) */
    @Operation(summary = "조건 검색 (유형/상태/키워드)")
    @GetMapping("/search")
    public Page<ShAnnouncementResponse> search(
            @RequestParam(required = false) SHHousingCategory category, // 주택유형 (임대/분양)
            @RequestParam(required = false) RecruitStatus status,     // 모집상태 (진행중/완료)
            @RequestParam(required = false, defaultValue = "") String keyword,
            @ParameterObject
            @PageableDefault(sort = "postDate", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return service.search(category, status, keyword, pageable);
    }

    /** 최근 7일 내 등록된 공고 */
    @Operation(summary = "최근 공고 조회 (7일 내 등록)")
    @GetMapping("/recent")
    public Page<ShAnnouncementResponse> recent(
            @ParameterObject
            @PageableDefault(sort = "postDate", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return service.getRecent(pageable);
    }

    /** 청년 친화형 공고 추천 (지역 필터 선택 가능) */
    @Operation(summary = "청년 친화형 공고 추천 (지역 필터 선택적)")
    @GetMapping("/recommend")
    public Page<ShAnnouncementResponse> recommend(
            @RequestParam(required = false, defaultValue = "") String region,
            @ParameterObject
            @PageableDefault(size = 10, sort = "postDate", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return region.isBlank()
                ? service.getYouthRecommendations(pageable)
                : service.getYouthRecommendations(region, pageable);
    }

    /** 사용자 맞춤 LH 주택 공고 추천 */
    @Operation(summary = "사용자 맞춤 SH 주거 공고 추천", description = """
    사용자의 나이, 지역, 소득 구간(중위소득 100~300%)을 기준으로 맞춤형 SH공사 주거 공고를 추천합니다.

    추천 로직 요약:
    - 청년층(20~35세) 및 저소득층(중위소득 150% 이하): 임대·행복주택 중심 추천
    - 중위·고소득층(200~300%): 분양·공공분양 중심 추천
    - 거주 지역과 공고 지역이 일치할수록 우선순위 상승
    - 게시일이 최근일수록 가중치 부여
    - 내부적으로 산출된 종합 점수에 따라 상위 10개의 추천 상품 반환
    """)
    @GetMapping("/recommend/{userId}")
    public List<ShAnnouncementResponse> recommendForUser(
            @Parameter(description = "사용자 ID") @PathVariable Integer userId,
            @Parameter(description = "지역 일치 여부 (true=시·도 완전 일치, false=광역 단위 포함)")
            @RequestParam(defaultValue = "false") boolean strictRegionMatch
    ) {
        return service.recommendForUser(userId, strictRegionMatch);
    }
}
