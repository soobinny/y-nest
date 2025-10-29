package com.example.capstonedesign.domain.shannouncements.service;

import com.example.capstonedesign.domain.shannouncements.dto.response.ShAnnouncementResponse;
import com.example.capstonedesign.domain.shannouncements.entity.RecruitStatus;
import com.example.capstonedesign.domain.shannouncements.entity.SHHousingCategory;
import com.example.capstonedesign.domain.shannouncements.repository.ShAnnouncementRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ShAnnouncementService
 * -----------------------------------------------------
 * - SH공사 청년주거 공고 조회 비즈니스 로직
 * - 페이징, 조건 검색, 추천 등 처리
 */
@Service
@RequiredArgsConstructor
public class ShAnnouncementService {

    private final ShAnnouncementRepository repo;

    /** 전체 공고 조회 (페이징) */
    public Page<ShAnnouncementResponse> getAll(Pageable pageable) {
        return repo.findAll(pageable)
                .map(ShAnnouncementResponse::fromEntity);
    }

    /** 조건 검색 (공급유형 / 진행상태 / 키워드) */
    public Page<ShAnnouncementResponse> search(SHHousingCategory category, RecruitStatus status, String keyword, Pageable pageable) {
        return repo.findAll((root, cq, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (category != null)
                preds.add(cb.equal(root.get("category"), category.name()));      // Enum → 문자열 비교
            if (status != null)
                preds.add(cb.equal(root.get("recruitStatus"), status.name()));   // Enum → 문자열 비교
            if (keyword != null && !keyword.isBlank())
                preds.add(cb.like(root.get("title"), "%" + keyword + "%"));
            return cb.and(preds.toArray(new Predicate[0]));
        }, pageable).map(ShAnnouncementResponse::fromEntity);
    }

    /** 최근 7일 내 등록된 공고 조회 */
    public Page<ShAnnouncementResponse> getRecent(Pageable pageable) {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        return repo.findAll((root, cq, cb) ->
                        cb.greaterThanOrEqualTo(root.get("postDate"), sevenDaysAgo),
                pageable
        ).map(ShAnnouncementResponse::fromEntity);
    }

    /** 청년 친화형 공고 추천 (전체) */
    public Page<ShAnnouncementResponse> getYouthRecommendations(Pageable pageable) {
        List<String> youthTypes = List.of(
                "청년안심주택", "행복주택", "사회주택", "두레주택",
                "도시형생활주택", "신혼희망타운", "공공분양"
        );

        return repo.findAll((root, cq, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(root.get("supplyType").in(youthTypes));      // 청년형 공급유형
            preds.add(cb.equal(root.get("recruitStatus"), "now")); // 진행 중만
            return cb.and(preds.toArray(new Predicate[0]));
        }, pageable).map(ShAnnouncementResponse::fromEntity);
    }

    /** 청년 친화형 공고 추천 (지역 필터 포함) */
    public Page<ShAnnouncementResponse> getYouthRecommendations(String region, Pageable pageable) {
        List<String> youthTypes = List.of(
                "청년안심주택", "행복주택", "사회주택", "두레주택",
                "도시형생활주택", "신혼희망타운", "공공분양"
        );

        return repo.findAll((root, cq, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(root.get("supplyType").in(youthTypes));
            preds.add(cb.equal(root.get("recruitStatus"), "now"));
            if (region != null && !region.isBlank())
                preds.add(cb.like(root.get("region"), "%" + region + "%"));
            return cb.and(preds.toArray(new Predicate[0]));
        }, pageable).map(ShAnnouncementResponse::fromEntity);
    }
}
