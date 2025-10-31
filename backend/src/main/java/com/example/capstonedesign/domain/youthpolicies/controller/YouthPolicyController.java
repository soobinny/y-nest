package com.example.capstonedesign.domain.youthpolicies.controller;

import com.example.capstonedesign.domain.youthpolicies.dto.response.YouthPolicyResponse;
import com.example.capstonedesign.domain.youthpolicies.service.YouthPolicyQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * YouthPolicyController
 * -------------------------------------------------
 * 청년정책 조회 및 추천용 컨트롤러
 * - 정책 목록 / 최근 등록 / 마감 임박 / 맞춤 추천 / 상세 조회 API 제공
 */
@Tag(name = "Youth Policies", description = "청년정책 공고 조회 및 사용자 맞춤 추천 API")
@RestController
@RequestMapping("/api/youth-policies")
@RequiredArgsConstructor
public class YouthPolicyController {

    private final YouthPolicyQueryService queryService;

    /** 정책 목록 조회 (검색 + 페이징) */
    @Operation(summary = "청년정책 목록 조회", description = "DB에 저장된 청년정책 목록을 조회합니다.")
    @GetMapping
    public Page<YouthPolicyResponse> getPolicies(
            @Parameter(description = "검색 키워드 (정책명 또는 기관명)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "지역코드 (예: 11000)")
            @RequestParam(required = false) String regionCode,
            @ParameterObject Pageable pageable
    ) {
        if (pageable == null || pageable.isUnpaged()) {
            pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return queryService.getPaged(keyword, regionCode, pageable);
    }

    /** 최근 30일 내 등록된 정책 조회 */
    @Operation(summary = "최근 공고 조회 (30일 이내 등록)", description = "최근 한 달 이내에 등록된 청년정책을 조회합니다.")
    @GetMapping("/recent")
    public Page<YouthPolicyResponse> getRecentPolicies(@ParameterObject Pageable pageable) {
        return queryService.getRecentPolicies(pageable);
    }

    /** 마감 임박 정책 조회 (7일 내 종료 예정) */
    @Operation(summary = "마감 임박 공고 조회 (7일 내 마감)", description = "7일 내 마감되는 청년정책 목록을 조회합니다.")
    @GetMapping("/closing-soon")
    public Page<YouthPolicyResponse> getClosingSoonPolicies(@ParameterObject Pageable pageable) {
        return queryService.getClosingSoonPolicies(pageable);
    }

    /** 사용자 맞춤 추천 정책 조회 */
    @Operation(summary = "사용자 맞춤 정책 추천", description = """
    사용자의 나이, 지역, 소득 구간(중위소득 100~300%)을 기준으로 맞춤형 청년정책을 추천합니다.

    추천 로직 요약:
    - 청년층(19~34세): 취업·창업·주거 관련 정책을 중심으로 추천
    - 중장년층(35~49세): 생계·가족·금융 지원 정책 우선 추천
    - 저소득층(중위소득 150% 이하): 소득 보전 및 임대 지원 정책 강화
    - 중산층(중위소득 200~300%): 자산 형성·저축·교육 지원 정책 중심 추천
    - 지역 일치 정책은 우선순위를 높여 표시하며, 전국 단위 정책은 보조적으로 포함됨
    - 내부적으로 산출된 종합 점수에 따라 상위 10개의 추천 상품 반환
    """)
    @GetMapping("/recommend/{userId}")
    public List<YouthPolicyResponse> recommendPolicies(
            @Parameter(description = "사용자 ID") @PathVariable Integer userId,
            @Parameter(description = "지역 일치 여부 (true=시·군·구만, false=광역시·도 포함)")
            @RequestParam(defaultValue = "false") boolean strictRegionMatch
    ) {
        return queryService.recommendForUser(userId, strictRegionMatch);
    }

    /** 단일 정책 상세 조회 */
    @Operation(summary = "청년정책 상세 조회", description = "정책 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public YouthPolicyResponse getPolicyById(@Parameter(description = "정책 ID") @PathVariable Long id) {
        return queryService.getById(id);
    }
}
