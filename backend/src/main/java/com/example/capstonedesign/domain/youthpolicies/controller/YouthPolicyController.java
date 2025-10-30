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

    /** 최근 7일 내 등록된 정책 조회 */
    @Operation(summary = "최근 공고 조회 (7일 이내 등록)", description = "최근 일주일 내 등록된 청년정책을 조회합니다.")
    @GetMapping("/recent")
    public Page<YouthPolicyResponse> getRecentPolicies(@ParameterObject Pageable pageable) {
        return queryService.getRecentPolicies(pageable);
    }

    /** 마감 임박 정책 조회 (3일 내 종료 예정) */
    @Operation(summary = "마감 임박 공고 조회 (3일 내 마감)", description = "3일 내 마감되는 청년정책 목록을 조회합니다.")
    @GetMapping("/closing-soon")
    public Page<YouthPolicyResponse> getClosingSoonPolicies(@ParameterObject Pageable pageable) {
        return queryService.getClosingSoonPolicies(pageable);
    }

    /** 사용자 맞춤 추천 정책 조회 */
    @Operation(
            summary = "사용자 맞춤 정책 추천",
            description = """
            회원의 나이, 지역, 소득대역 정보를 기반으로 개인 맞춤형 청년정책을 추천합니다.
            <br><br>
            <b>strictRegionMatch 옵션 설명</b><br>
            - <code>true</code>: 해당 시·군·구 지역 코드만 포함된 정책만 표시 (전국 단위 제외)<br>
            - <code>false</code> (기본값): 사용자의 광역시·도 단위 포함 정책도 표시
            """
    )
    @GetMapping("/recommend/{userId}")
    public List<YouthPolicyResponse> recommendPolicies(
            @Parameter(description = "사용자 ID") @PathVariable Integer userId,
            @Parameter(description = "지역 일치 기준 (true=시·군·구만, false=광역시·도 포함)", example = "false")
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
