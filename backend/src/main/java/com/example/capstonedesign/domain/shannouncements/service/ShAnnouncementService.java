package com.example.capstonedesign.domain.shannouncements.service;

import com.example.capstonedesign.domain.shannouncements.dto.response.ShAnnouncementResponse;
import com.example.capstonedesign.domain.shannouncements.entity.RecruitStatus;
import com.example.capstonedesign.domain.shannouncements.entity.SHHousingCategory;
import com.example.capstonedesign.domain.shannouncements.entity.ShAnnouncement;
import com.example.capstonedesign.domain.shannouncements.repository.ShAnnouncementRepository;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final UsersRepository usersRepository;

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

    /**
     * 사용자 맞춤형 SH 주거 공고 추천
     * -------------------------------------------------
     * - 사용자의 나이, 지역, 소득 구간(중위소득 100~300%)을 기준으로
     *   SH공사 공고를 맞춤 추천합니다.
     * - 청년층 및 저소득층 → 임대/청년안심주택 중심
     * - 중위·고소득층 → 분양·공공분양 중심
     * - 지역 일치 및 마감 임박도에 가중치 적용
     * - 종합 점수(score)가 낮을수록 상위 노출
     */
    public List<ShAnnouncementResponse> recommendForUser(Integer userId, boolean strictRegionMatch) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int age = user.getAge();
        String region = user.getRegion();
        String incomeBand = user.getIncome_band().replace(" ", "");

        // 모든 공고 데이터 로드
        List<ShAnnouncement> all = repo.findAll();

        return all.stream()
                .filter(a -> matchByIncomeAndAge(a, incomeBand, age))
                .filter(a -> matchRegion(region, a.getRegion(), strictRegionMatch))
                .map(a -> {
                    double score = calculateScore(age, incomeBand, region, a);
                    String reason = buildReason(age, incomeBand, region, a);

                    // attachments JSON 파싱
                    List<Map<String, String>> attachments = ShAnnouncementResponse
                            .fromEntity(a)
                            .getAttachments();

                    return ShAnnouncementResponse.builder()
                            .id(a.getId())
                            .title(a.getTitle())
                            .department(a.getDepartment())
                            .postDate(a.getPostDate())
                            .views(a.getViews())
                            .recruitStatus(a.getRecruitStatus())
                            .supplyType(a.getSupplyType())
                            .attachments(attachments)
                            .score(score)
                            .reason(reason)
                            .build();
                })
                .sorted(Comparator.comparingDouble(ShAnnouncementResponse::getScore))
                .limit(10)
                .collect(Collectors.toList());
    }

    // --------------------------------------------------------
    // 내부 로직
    // --------------------------------------------------------

    /** 소득 및 연령대 기반 필터링 */
    private boolean matchByIncomeAndAge(ShAnnouncement a, String incomeBand, int age) {
        boolean isYouth = age <= 35;
        boolean isLowIncome = incomeBand != null && (incomeBand.contains("100") || incomeBand.contains("150"));

        if (isYouth && isLowIncome)
            return containsAny(a.getSupplyType(), "청년", "행복");
        if (isYouth)
            return containsAny(a.getSupplyType(), "행복", "공공");
        if (incomeBand != null && incomeBand.contains("300"))
            return a.getSupplyType() != null && a.getSupplyType().contains("분양");
        return true;
    }

    /** 지역 일치 여부 */
    private boolean matchRegion(String userRegion, String annRegion, boolean strict) {
        if (userRegion == null || annRegion == null) return true;
        return strict
                ? annRegion.startsWith(userRegion)
                : annRegion.startsWith(userRegion.substring(0, 2));
    }

    /** 점수 계산 (낮을수록 상위) */
    private double calculateScore(int age, String incomeBand, String region, ShAnnouncement a) {
        double score = 50.0; // 기본값 (낮을수록 상위)

        // 지역 일치 시 가점
        if (a.getRegion() != null && region != null &&
                a.getRegion().startsWith(region.substring(0, 2))) {
            score -= 10;
        }

        // 소득대역별 가중치 (소득 낮을수록 점수 하락)
        score -= switch (incomeBand) {
            case "중위소득100%이하" -> 10;
            case "중위소득150%이하" -> 7;
            case "중위소득200%이하" -> 4;
            case "중위소득300%이하" -> 2;
            default -> 0;
        };

        // 연령대별 우대 (청년/노년층에 가점)
        if (age < 30) score -= 5;
        else if (age > 60) score -= 3;

        // 최근 등록(7일 이내) 공고 가점
        if (a.getPostDate() != null) {
            long daysSincePost = ChronoUnit.DAYS.between(a.getPostDate(), LocalDate.now());
            if (daysSincePost >= 0 && daysSincePost <= 7) {
                score -= (7 - daysSincePost) * 1.5; // 최근일수록 가점↑
            }
        }

        // 최소 점수 1점 방어
        return Math.max(score, 1);
    }

    /** 추천 이유 구성 */
    private String buildReason(int age, String incomeBand, String region, ShAnnouncement a) {
        StringBuilder reason = new StringBuilder();

        // 지역
        if (region != null && a.getRegion() != null &&
                a.getRegion().startsWith(region.substring(0, 2))) {
            reason.append("거주 지역과 동일 지역 공고, ");
        } else {
            reason.append("타지역 공고이지만 조건 적합, ");
        }

        // 소득
        if (incomeBand != null) {
            if (incomeBand.contains("100") || incomeBand.contains("150")) {
                reason.append("저소득층 청년 대상 공고, ");
            } else if (incomeBand.contains("200") || incomeBand.contains("300")) {
                reason.append("중산층 이상을 위한 분양형 공고, ");
            }
        }

        // 연령
        if (age < 30) {
            reason.append("청년층 우대 대상, ");
        } else if (age > 60) {
            reason.append("노년층 우대 대상, ");
        }

        // 게시일 기준 (신규)
        if (a.getPostDate() != null &&
                ChronoUnit.DAYS.between(a.getPostDate(), LocalDate.now()) <= 3) {
            reason.append("최근 등록된 신규 공고");
        }

        // 마지막 쉼표 제거
        return reason.toString().replaceAll(", $", "");
    }

    /** 문자열 포함 검사 (null-safe) */
    private boolean containsAny(String target, String... keywords) {
        if (target == null) return false;
        for (String k : keywords) {
            if (target.contains(k)) return true;
        }
        return false;
    }
}
