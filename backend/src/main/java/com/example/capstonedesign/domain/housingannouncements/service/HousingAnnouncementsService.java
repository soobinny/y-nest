package com.example.capstonedesign.domain.housingannouncements.service;

import com.example.capstonedesign.domain.housingannouncements.dto.response.HousingAnnouncementsResponse;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingAnnouncements;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingStatus;
import com.example.capstonedesign.domain.housingannouncements.repository.HousingAnnouncementsRepository;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * HousingAnnouncementsService
 * -----------------------------------------------------
 * 역할
 * - LH(한국토지주택공사) 주택 공고 데이터를 조회하는 비즈니스 로직을 담당
 * - 페이징, 정렬, 필터링(카테고리·상태·지역) 및 조건 기반 조회 기능 제공
 * - Swagger UI 에서 발생할 수 있는 비정상 sort 파라미터 문제 방어(sanitizePageable)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HousingAnnouncementsService {

    private final HousingAnnouncementsRepository repository;
    private final UsersRepository usersRepository;

    // --------------------------------------------------------
    // 전체 공고 조회
    // --------------------------------------------------------

    /**
     * 전체 LH 주택 공고 목록을 조회
     *
     * @param pageable 페이징 및 정렬 정보
     * @return HousingAnnouncementsResponse 페이지 객체
     *
     * 기본 정렬: 공고일(noticeDate) 기준 내림차순
     */
    public Page<HousingAnnouncementsResponse> getAll(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable, "noticeDate", Sort.Direction.DESC);

        return repository.findAll(safePageable)
                .map(this::toResponse);
    }

    // --------------------------------------------------------
    // 조건 검색
    // --------------------------------------------------------

    /**
     * 카테고리, 상태, 지역명을 기준으로 공고를 검색
     *
     * @param category  주택 유형 (임대주택 / 분양주택 등)
     * @param status    공고 상태 (공고중 / 마감 등)
     * @param region    지역명 (예: 서울, 부산 등)
     * @param pageable  페이징 및 정렬 정보
     * @return 조건에 맞는 HousingAnnouncementsResponse 페이지 객체
     * <p>
     * null 값이 들어오면 다음 기본값으로 보정:
     * - category: 임대주택
     * - status: 공고중
     * - region: ""
     */
    public Page<HousingAnnouncementsResponse> search(
            HousingCategory category,
            HousingStatus status,
            String region,
            Pageable pageable
    ) {
        Pageable safePageable = sanitizePageable(pageable, "noticeDate", Sort.Direction.DESC);

        // null 방어 기본값 설정
        HousingCategory safeCategory = category != null ? category : HousingCategory.임대주택;
        HousingStatus safeStatus = status != null ? status : HousingStatus.공고중;
        String safeRegion = region != null ? region : "";

        return repository.findByCategoryAndStatusAndRegionNameContaining(
                        safeCategory, safeStatus, safeRegion, safePageable)
                .map(this::toResponse);
    }

    // --------------------------------------------------------
    // 마감 임박 공고 조회
    // --------------------------------------------------------

    /**
     * 마감일(closeDate)이 오늘로부터 3일 이내인 공고를 조회
     *
     * @param pageable 페이징 및 정렬 정보
     * @return 3일 내 마감 예정인 공고 목록
     *
     * 기본 정렬: 마감일 오름차순 (가장 임박한 순)
     */
    public Page<HousingAnnouncementsResponse> getClosingSoon(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable, "closeDate", Sort.Direction.ASC);

        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);

        return repository.findByCloseDateBetween(today, threeDaysLater, safePageable)
                .map(this::toResponse);
    }

    // --------------------------------------------------------
    // 최근 등록 공고 조회
    // --------------------------------------------------------

    /**
     * 최근 7일 이내에 등록된 공고 조회
     *
     * @param pageable 페이징 및 정렬 정보
     * @return 최근 등록된 공고 목록
     *
     * 기본 정렬: 공고일 내림차순 (최신 순)
     */
    public Page<HousingAnnouncementsResponse> getRecent(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable, "noticeDate", Sort.Direction.DESC);

        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        return repository.findByNoticeDateAfter(sevenDaysAgo, safePageable)
                .map(this::toResponse);
    }

    // --------------------------------------------------------
    // 사용자 맞춤형 추천 공고 조회
    // --------------------------------------------------------

    /**
     * 사용자 맞춤형 LH 주택 공고 추천
     * -------------------------------------------------
     * - 사용자의 나이, 지역, 소득대역(income_band)을 기반으로 맞춤 추천
     * - 저소득층: 임대주택 중심 / 중산층: 분양주택 중심
     * - 지역 일치 여부에 따라 가중치 부여
     * - 점수(score)가 낮을수록 추천 순위가 높음
     */
    public List<HousingAnnouncementsResponse> recommendForUser(Integer userId, boolean strictRegionMatch) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int age = user.getAge();
        String region = user.getRegion();
        String incomeBand = user.getIncome_band().replace(" ", "");

        // 전체 공고 데이터 로드
        List<HousingAnnouncements> all = repository.findAll();

        return all.stream()
                .filter(ha -> matchAge(ha.getCategory(), age))
                .filter(ha -> matchIncome(incomeBand, ha.getCategory()))
                .filter(ha -> matchRegion(region, ha.getRegionName(), strictRegionMatch))
                .map(ha -> {
                    double score = calculateRecommendationScore(age, incomeBand, region, ha);
                    String reason = getRecommendationReason(age, incomeBand, region, ha);

                    return HousingAnnouncementsResponse.builder()
                            .id(ha.getId())
                            .name(ha.getProduct().getName())
                            .provider(ha.getProduct().getProvider())
                            .regionName(ha.getRegionName())
                            .noticeDate(ha.getNoticeDate())
                            .closeDate(ha.getCloseDate())
                            .status(ha.getStatus())
                            .category(ha.getCategory())
                            .detailUrl(ha.getProduct().getDetailUrl())
                            .score(score)
                            .reason(reason)
                            .build();
                })
                .sorted(Comparator.comparingDouble(HousingAnnouncementsResponse::getScore))
                .limit(10)
                .toList();
    }

    // 나이별 선호 주택 유형 매칭
    private boolean matchAge(HousingCategory category, int age) {
        return switch (category) {
            case 임대주택 -> age < 35 || age > 60; // 청년·노년층 중심
            case 분양주택 -> age >= 30 && age <= 50;
            default -> true;
        };
    }

    // 소득대역별 매칭 (저소득층은 임대 위주, 고소득층은 분양 위주)
    private boolean matchIncome(String incomeBand, HousingCategory category) {
        if (incomeBand == null) return true;
        return switch (incomeBand) {
            case "중위소득100%이하", "중위소득150%이하" -> category == HousingCategory.임대주택;
            case "중위소득200%이하", "중위소득300%이하" -> category == HousingCategory.분양주택;
            default -> true;
        };
    }

    // 지역 매칭 (strictRegionMatch가 true면 시·도 완전 일치)
    private boolean matchRegion(String userRegion, String announcementRegion, boolean strict) {
        if (userRegion == null || announcementRegion == null) return true;
        return strict
                ? announcementRegion.startsWith(userRegion)
                : announcementRegion.startsWith(userRegion.substring(0, 2));
    }

    // 추천 점수 계산 (낮을수록 상위)
    private double calculateRecommendationScore(int age, String incomeBand, String region, HousingAnnouncements ha) {
        double score = 50.0; // 기본값 (낮을수록 상위)

        // 지역 일치 시 가점
        if (ha.getRegionName() != null && region != null &&
                ha.getRegionName().startsWith(region.substring(0, 2))) {
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

        // 마감 임박(7일 이내) 공고 우대
        if (ha.getCloseDate() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), ha.getCloseDate());
            if (daysLeft <= 7 && daysLeft >= 0) {
                score -= (7 - daysLeft) * 1.5; // 임박할수록 가점↑
            }
        }

        // 1점 이하로 떨어지지 않도록 방어
        return Math.max(score, 1);
    }

    // 추천 이유 문자열 구성
    private String getRecommendationReason(int age, String incomeBand, String region, HousingAnnouncements ha) {
        StringBuilder reason = new StringBuilder();

        // 지역
        if (region != null && ha.getRegionName() != null &&
                ha.getRegionName().startsWith(region.substring(0, 2))) {
            reason.append("거주 지역과 동일 지역인 공고, ");
        } else {
            reason.append("타지역 공고이지만 조건 적합, ");
        }

        // 소득
        if (incomeBand != null) {
            if (incomeBand.contains("100") || incomeBand.contains("150")) {
                reason.append("저소득층 대상 임대주택 우선 추천, ");
            } else if (incomeBand.contains("200") || incomeBand.contains("300")) {
                reason.append("중산층을 위한 분양주택 추천, ");
            }
        }

        // 연령
        if (age < 30) {
            reason.append("청년층 우대 대상, ");
        } else if (age > 60) {
            reason.append("노년층 우대 대상, ");
        }

        // 마감임박
        if (ha.getCloseDate() != null && ChronoUnit.DAYS.between(LocalDate.now(), ha.getCloseDate()) <= 3) {
            reason.append("마감 임박 공고");
        }

        // 마지막 쉼표 제거
        return reason.toString().replaceAll(", $", "");
    }

    // --------------------------------------------------------
    // 내부 유틸리티
    // --------------------------------------------------------

    /**
     * Entity → DTO 변환 메서드
     * ---------------------------------------------------------
     * - HousingAnnouncements 엔티티를 Response DTO로 변환
     * - 일반 목록/검색/조회 API에서 사용
     */
    private HousingAnnouncementsResponse toResponse(HousingAnnouncements ha) {
        return HousingAnnouncementsResponse.builder()
                .id(ha.getId())
                .name(ha.getProduct().getName())
                .provider(ha.getProduct().getProvider())
                .regionName(ha.getRegionName())
                .noticeDate(ha.getNoticeDate())
                .closeDate(ha.getCloseDate())
                .status(ha.getStatus())
                .category(ha.getCategory())
                .detailUrl(ha.getProduct().getDetailUrl())
                .build();
    }

    /**
     * Pageable 보정 유틸
     * ---------------------------------------------------------
     * Swagger 테스트 시 발생하는 비정상 sort 값(s=["string"])을 필터링하고
     * 잘못된 정렬 요청 시 안전한 기본 정렬(defaultSort)로 교정함
     *
     * @param pageable     요청으로 들어온 Pageable 객체
     * @param defaultSort  기본 정렬 기준 컬럼명
     * @param direction    기본 정렬 방향
     * @return 안전하게 보정된 Pageable 객체
     */
    private Pageable sanitizePageable(Pageable pageable, String defaultSort, Sort.Direction direction) {
        if (pageable == null) {
            // 페이지 정보가 없을 경우 기본값(page=0, size=10)
            return PageRequest.of(0, 10, Sort.by(direction, defaultSort));
        }

        // 잘못된 sort 파라미터 필터링
        Sort filteredSort = Sort.by(
                pageable.getSort().stream()
                        .filter(order -> {
                            String prop = order.getProperty();
                            return prop != null
                                    && !prop.contains("[")
                                    && !prop.contains("]")
                                    && !"string".equalsIgnoreCase(prop);
                        })
                        .toList()
        );

        // 정렬값이 완전히 비었거나 유효하지 않으면 기본 정렬로 교체
        if (filteredSort.isUnsorted() || !filteredSort.iterator().hasNext()) {
            filteredSort = Sort.by(direction, defaultSort);
        }

        // 보정된 Pageable 반환
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), filteredSort);
    }
}
