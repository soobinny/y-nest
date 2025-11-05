package com.example.capstonedesign.domain.notifications.controller;

import com.example.capstonedesign.domain.notifications.dto.RecentNoticeDto;
import com.example.capstonedesign.domain.notifications.service.RecentNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * RecentNoticeController
 * -------------------------------------------------
 * 홈 화면용 "최근 게시물" 통합 조회 API
 * - 주거(LH, SH), 정책 데이터를 하나로 묶어 반환
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
@Tag(name = "최근 게시물", description = "홈 화면 통합 최신 게시물 조회 API")
public class RecentNoticeController {

    private final RecentNoticeService recentNoticeService;

    /**
     * 최근 게시물 조회 API
     * -------------------------------------------------
     * [GET] /api/notices/recent
     * - 주거 + 정책 최신 게시물 통합 반환
     * - 정렬 기준: createdAt (최신순)
     */
    @GetMapping("/recent")
    @Operation(summary = "최근 게시물 조회", description = "주거, 정책, 전체 데이터를 각각 반환")
    public Map<String, List<RecentNoticeDto>> getRecentNotices() {
        return recentNoticeService.getRecentNotices();
    }
}
