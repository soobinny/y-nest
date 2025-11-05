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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * RecentNoticeService
 * -------------------------------------------------
 * 홈 화면의 "최근 게시물" 데이터를 통합 조회하는 서비스
 * - 주거(LH·SH) + 정책 데이터를 통합 정렬
 * - 금융 공고는 게시일이 없어 제외
 */
@Service
@RequiredArgsConstructor
public class RecentNoticeService {

    private final LhNoticeRepository lhRepo;
    private final ShAnnouncementRepository shRepo;
    private final YouthPolicyRepository policyRepo;

    /**
     * 최신 게시물 통합 조회
     * -------------------------------------------------
     * ① LH 공고 (게시일 기준)
     * ② SH 공고 (게시일 기준)
     * ③ 청년정책 (시작일 기준)
     * → 통합 정렬 후 상위 N개 반환
     */
    public List<RecentNoticeDto> getRecentNotices() {

        // LH 공고: 게시일 기준 내림차순 정렬 후 상위 5건
        var lhList = lhRepo.findTop20ByOrderByPanNtStDtDesc()
                .stream()
                .map(RecentNoticeDto::fromLh)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RecentNoticeDto::getCreatedAt).reversed())
                .limit(5)
                .toList();

        // SH 공고: 게시일(postDate) 기준 내림차순 정렬 후 상위 5건
        var shList = shRepo.findTop20ByOrderByPostDateDesc()
                .stream()
                .map(RecentNoticeDto::fromSh)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RecentNoticeDto::getCreatedAt).reversed())
                .limit(5)
                .toList();

        // 청년정책: 오늘 이후 종료되지 않은 정책 중 시작일 최신순 상위 5건
        var todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var policyList = policyRepo.findActiveOrderByStartDateDesc(todayStr, PageRequest.of(0, 100))
                .stream()
                .map(RecentNoticeDto::fromPolicy)  // 내부에서 end<today 필터링됨
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RecentNoticeDto::getCreatedAt).reversed())
                .limit(5)
                .toList();

        // 통합 리스트 생성: (LH + SH + 정책)
        return Stream.of(lhList, shList, policyList)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .filter(dto -> dto.getCreatedAt() != null)
                .sorted(Comparator.comparing(
                        RecentNoticeDto::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .limit(5) // 전체 상위 5건만 노출
                .toList();
    }
}
