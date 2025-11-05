package com.example.capstonedesign.domain.notifications.service;

import com.example.capstonedesign.domain.housingannouncements.repository.LhNoticeRepository;
import com.example.capstonedesign.domain.notifications.dto.RecentNoticeDto;
import com.example.capstonedesign.domain.shannouncements.repository.ShAnnouncementRepository;
import com.example.capstonedesign.domain.youthpolicies.repository.YouthPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RecentNoticeService
 * -------------------------------------------------
 * 홈 화면의 "최근 게시물" 통합 조회 서비스
 * - 전체: 주거 + 정책 최신 5개
 * - 주거: LH + SH 최신 5개
 * - 정책: 정책 최신 5개
 */
@Service
@RequiredArgsConstructor
public class RecentNoticeService {

    private final LhNoticeRepository lhRepo;
    private final ShAnnouncementRepository shRepo;
    private final YouthPolicyRepository policyRepo;

    public Map<String, List<RecentNoticeDto>> getRecentNotices() {

        // LH 공고
        var lhList = lhRepo.findTop20ByOrderByPanNtStDtDesc()
                .stream()
                .map(RecentNoticeDto::fromLh)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RecentNoticeDto::getCreatedAt).reversed())
                .limit(5)
                .toList();

        // SH 공고
        var shList = shRepo.findTop20ByOrderByPostDateDesc()
                .stream()
                .map(RecentNoticeDto::fromSh)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RecentNoticeDto::getCreatedAt).reversed())
                .limit(5)
                .toList();

        // 청년정책
        var todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var policyList = policyRepo.findActiveOrderByStartDateDesc(todayStr, PageRequest.of(0, 100))
                .stream()
                .map(RecentNoticeDto::fromPolicy)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RecentNoticeDto::getCreatedAt).reversed())
                .limit(5)
                .toList();

        // 주거 = LH + SH 통합 후 최신순 5개
        var housingList = Stream.concat(lhList.stream(), shList.stream())
                .sorted(Comparator.comparing(RecentNoticeDto::getCreatedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // 전체 = 주거 + 정책 통합 후 최신순 5개
        var allList = Stream.concat(housingList.stream(), policyList.stream())
                .sorted(Comparator.comparing(RecentNoticeDto::getCreatedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // 카테고리별 리스트를 Map으로 반환
        Map<String, List<RecentNoticeDto>> result = new HashMap<>();
        result.put("all", allList);
        result.put("housing", housingList);
        result.put("policy", policyList);

        return result;
    }
}