package com.example.capstonedesign.domain.housingannouncements.service;

import com.example.capstonedesign.domain.housingannouncements.dto.response.HousingAnnouncementsResponse;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingStatus;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingSubCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.LhNotice;
import com.example.capstonedesign.domain.housingannouncements.repository.LhNoticeRepository;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * HousingAnnouncementsService (LH Notice 기반)
 * -----------------------------------------------------
 * LH(한국토지주택공사) 공고 데이터를 처리하는 서비스 계층
 * <p>
 * 주요 기능:
 * - LH 공고 전체 조회 및 조건 검색 (카테고리, 상태, 키워드 기반)
 * - 마감 임박/최근 등록 공고 조회
 * - 사용자 맞춤형 추천 (나이·소득·지역 기반)
 * - LhNotice 엔티티 → HousingAnnouncementsResponse DTO 매핑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HousingAnnouncementsService {

    private final LhNoticeRepository lhNoticeRepository;
    private final UsersRepository usersRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    // --------------------------------------------------------
    // [1] 전체 공고 조회
    // --------------------------------------------------------
    /**
     * LH 공고 전체 조회 (페이징 지원)
     * - 기본 정렬: 게시일(panNtStDt) 내림차순
     * @param pageable 요청된 페이지 및 정렬 정보
     * @return 공고 목록 (Page 형태)
     */
    public Page<HousingAnnouncementsResponse> getAll(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable, "panNtStDt", Sort.Direction.DESC);
        return lhNoticeRepository.findAll(safePageable)
                .map(this::toResponseFromLh);
    }

    // --------------------------------------------------------
    // [2] 조건 검색
    // --------------------------------------------------------
    /**
     * LH 공고 조건 검색
     * ---------------------------------------------------------
     * - category: 임대/분양 등 주택 유형
     * - status: 공고 상태 (공고중/접수중/마감 등)
     * - keyword: 공고명, 지역명, 카테고리명 중 하나라도 일치 시 검색
     * @param category 주택 유형 필터
     * @param status 공고 상태 필터
     * @param keyword 검색 키워드
     * @param pageable 페이지 및 정렬 정보
     * @return 조건에 맞는 공고 목록 페이지
     */
    public Page<HousingAnnouncementsResponse> search(
            HousingCategory category,
            HousingStatus status,
            String keyword,
            Pageable pageable
    ) {
        Pageable safePageable = sanitizePageable(pageable, "panNtStDt", Sort.Direction.DESC);
        String safeKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        // Enum → 실제 LH 데이터 문자열 매핑
        String safeCategory = (category != null) ? switch (category) {
            case 임대주택 -> "임대주택";
            case 분양주택 -> "분양주택";
            case 상가 -> "상가";
            case 토지 -> "토지";
            case 주거복지 -> "주거복지";
            default -> null;
        } : null;

        String safeStatus = (status != null) ? switch (status) {
            case 공고중 -> "공고중";
            case 정정공고중 -> "정정공고중";
            case 접수중 -> "접수중";
            case 접수마감 -> "접수마감";
            case 모집완료 -> "모집완료";
            case 종료 -> "종료";
        } : null;

        Page<LhNotice> result = lhNoticeRepository.searchNotices(
                safeCategory, safeStatus, safeKeyword, safePageable
        );

        return result.map(this::toResponseFromLh);
    }

    // --------------------------------------------------------
    // [3] 마감 임박 공고 조회
    // --------------------------------------------------------
    /**
     * 마감 3일 이내의 LH 공고 조회
     * ---------------------------------------------------------
     * - 오늘 기준 3일 내 마감되는 공고만 반환
     * - 정렬 기준: 마감일(clsgDt) 오름차순
     */
    public Page<HousingAnnouncementsResponse> getClosingSoon(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable, "clsgDt", Sort.Direction.ASC);

        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);

        List<HousingAnnouncementsResponse> filtered = lhNoticeRepository.findAll().stream()
                .filter(n -> parseDate(n.getClsgDt()) != null)
                .filter(n -> {
                    LocalDate close = parseDate(n.getClsgDt());
                    return !close.isBefore(today) && !close.isAfter(threeDaysLater);
                })
                .sorted(Comparator.comparing(n -> parseDate(n.getClsgDt())))
                .map(this::toResponseFromLh)
                .toList();

        return toPage(filtered, safePageable);
    }

    // --------------------------------------------------------
    // [4] 최근 등록 공고 조회
    // --------------------------------------------------------
    /**
     * 최근 7일 이내 등록된 LH 공고 조회
     * ---------------------------------------------------------
     * - 기준일: 게시일(panNtStDt)
     * - 정렬: 최신순 (내림차순)
     */
    public Page<HousingAnnouncementsResponse> getRecent(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable, "panNtStDt", Sort.Direction.DESC);

        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<HousingAnnouncementsResponse> recent = lhNoticeRepository.findAll().stream()
                .filter(n -> parseDate(n.getPanNtStDt()) != null)
                .filter(n -> !Objects.requireNonNull(parseDate(n.getPanNtStDt())).isBefore(sevenDaysAgo))
                .sorted(Comparator.comparing((LhNotice n) -> parseDate(n.getPanNtStDt())).reversed())
                .map(this::toResponseFromLh)
                .toList();

        return toPage(recent, safePageable);
    }

    // --------------------------------------------------------
    // [5] 사용자 맞춤형 공고 추천
    // --------------------------------------------------------
    /**
     * 사용자 정보 기반 맞춤형 공고 추천
     * ---------------------------------------------------------
     * - 입력: 사용자 나이, 지역, 소득대역
     * - 점수 계산 요소:
     *   · 지역 일치도 (동일 지역이면 가점)
     *   · 소득대역 (저소득층 우대)
     *   · 나이 (청년층 가점)
     *   · 마감 임박도
     * - 종합 점수(score) 기준 상위 10개 공고 반환
     */
    public List<HousingAnnouncementsResponse> recommendForUser(Integer userId, boolean strictRegionMatch) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int age = user.getAge();
        String region = user.getRegion();
        String incomeBand = user.getIncome_band().replace(" ", "");

        List<LhNotice> all = lhNoticeRepository.findAll();

        return all.stream()
                .map(n -> {
                    HousingCategory category = mapCategory(n.getUppAisTpNm());
                    HousingSubCategory subCategory = mapSubCategory(n.getAisTpCdNm());

                    double score = calculateRecommendationScore(age, incomeBand, region, n, category, subCategory, strictRegionMatch);
                    String reason = getRecommendationReason(age, incomeBand, region, n, category, subCategory, strictRegionMatch);

                    HousingAnnouncementsResponse r = toResponseFromLh(n);
                    r.setScore(score);
                    r.setReason(reason);
                    return r;
                })
                .sorted(Comparator.comparingDouble(HousingAnnouncementsResponse::getScore))
                .limit(10)
                .toList();
    }

    private HousingStatus mapStatus(String panSs) {
        if (panSs == null) return HousingStatus.공고중;

        String value = panSs.trim();

        return switch (value) {
            case "공고중" -> HousingStatus.공고중;
            case "접수중" -> HousingStatus.접수중;
            case "정정공고중" -> HousingStatus.정정공고중;
            case "접수마감" -> HousingStatus.접수마감;
            case "모집완료", "종료", "완료" -> HousingStatus.모집완료;
            default -> HousingStatus.종료;
        };
    }

    private HousingCategory mapCategory(String uppAisTpNm) {
        if (uppAisTpNm == null) return HousingCategory.기타;

        return switch (uppAisTpNm) {
            case "임대주택" -> HousingCategory.임대주택;
            case "분양주택", "공공분양(신혼희망)" -> HousingCategory.분양주택;
            case "상가" -> HousingCategory.상가;
            case "토지" -> HousingCategory.토지;
            case "주거복지" -> HousingCategory.주거복지;
            default -> HousingCategory.기타;
        };
    }

    private HousingSubCategory mapSubCategory(String aisTpCdNm) {
        if (aisTpCdNm == null) return HousingSubCategory.기타;
        return switch (aisTpCdNm) {
            case "국민임대" -> HousingSubCategory.국민임대;
            case "행복주택" -> HousingSubCategory.행복주택;
            case "공공임대" -> HousingSubCategory.공공임대;
            case "통합공공임대" -> HousingSubCategory.통합공공임대;
            case "영구임대" -> HousingSubCategory.영구임대;
            case "매입임대" -> HousingSubCategory.매입임대;
            case "분양주택" -> HousingSubCategory.분양주택;
            case "공공분양(신혼희망)", "행복주택(신혼희망)" -> HousingSubCategory.신혼희망;
            case "임대상가(입찰)", "임대상가(추첨)" -> HousingSubCategory.임대상가;
            case "토지" -> HousingSubCategory.토지;
            default -> HousingSubCategory.기타;
        };
    }

    /**
     * 종합 추천 점수 계산
     * ---------------------------------------------------------
     * 낮을수록 우선순위 ↑
     */
    private double calculateRecommendationScore(
            int age, String incomeBand, String region, LhNotice n,
            HousingCategory category, HousingSubCategory subCategory, boolean strictRegionMatch
    ) {
        double score = 50.0;

        // 1. 지역 일치도
        if (matchRegion(region, n.getCnpCdNm(), strictRegionMatch))
            score -= 15;

        // 2. 소득대역 점수
        score -= switch (incomeBand) {
            case "중위소득100%이하" -> 10;
            case "중위소득150%이하" -> 8;
            case "중위소득200%이하" -> 5;
            case "중위소득300%이하" -> 2;
            default -> 0;
        };

        // 3. 연령대 점수
        if (age < 30) score -= 8;       // 청년층
        else if (age <= 50) score -= 4; // 중장년
        else score -= 2;                // 노년층

        // 4. 마감 임박도 점수
        if (n.getClsgDt() != null) {
            LocalDate close = parseDate(n.getClsgDt());
            if (close != null) {
                long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), close);
                if (daysLeft <= 7 && daysLeft >= 0)
                    score -= (7 - daysLeft); // 7일 이내일수록 우대
            }
        }

        // 5. 유형 적합도 점수
        score += calculateTypeMatchScore(incomeBand, category, subCategory);

        return Math.max(score, 1);
    }

    /**
     * 소득대역과 주택 유형의 적합도 점수
     * ---------------------------------------------------------
     * 음수 = 우대 / 양수 = 비선호
     */
    private double calculateTypeMatchScore(String incomeBand, HousingCategory category, HousingSubCategory subCategory) {
        if (incomeBand == null) return 0;

        return switch (incomeBand) {
            case "중위소득100%이하" -> switch (category) {
                case 임대주택, 주거복지 -> -10;
                default -> (subCategory == HousingSubCategory.국민임대
                        || subCategory == HousingSubCategory.영구임대
                        || subCategory == HousingSubCategory.행복주택) ? -8 : +3;
            };
            case "중위소득150%이하" -> switch (category) {
                case 임대주택, 주거복지 -> -8;
                default -> (subCategory == HousingSubCategory.공공임대
                        || subCategory == HousingSubCategory.통합공공임대
                        || subCategory == HousingSubCategory.행복주택) ? -6 : +2;
            };
            case "중위소득200%이하" -> switch (category) {
                case 분양주택 -> -5;
                default -> (subCategory == HousingSubCategory.분양주택
                        || subCategory == HousingSubCategory.신혼희망) ? -4 : +1;
            };
            case "중위소득300%이하" -> switch (category) {
                case 분양주택, 상가, 토지 -> -3;
                default -> +2;
            };
            default -> 0;
        };
    }

    private String getRecommendationReason(
            int age, String incomeBand, String region, LhNotice n,
            HousingCategory category, HousingSubCategory subCategory, boolean strict
    ) {
        StringBuilder reason = new StringBuilder();

        // 지역
        if (matchRegion(region, n.getCnpCdNm(), strict))
            reason.append("거주 지역과 동일, ");
        else
            reason.append("타지역 공고, ");

        // 소득 및 유형
        if (incomeBand.contains("100") || incomeBand.contains("150")) {
            reason.append("저소득층 임대주택 우선, ");
        } else if (incomeBand.contains("200") || incomeBand.contains("300")) {
            reason.append("중산층 분양주택 중심, ");
        }

        // 연령대
        if (age < 30) reason.append("청년층 우대 대상, ");
        else if (age > 60) reason.append("노년층 우대 대상, ");

        // 마감 임박
        if (n.getClsgDt() != null && parseDate(n.getClsgDt()) != null &&
                ChronoUnit.DAYS.between(LocalDate.now(), parseDate(n.getClsgDt())) <= 3)
            reason.append("마감 임박 공고");

        // 여분의 콤마 제거
        return reason.toString().replaceAll(", $", "");
    }

    private boolean matchRegion(String userRegion, String annRegion, boolean strict) {
        if (userRegion == null || annRegion == null) return true;
        return strict
                ? annRegion.startsWith(userRegion)
                : annRegion.startsWith(userRegion.substring(0, 2));
    }

    // --------------------------------------------------------
    // [6] 내부 매핑 및 유틸리티
    // --------------------------------------------------------
    /**
     * LhNotice → HousingAnnouncementsResponse 변환
     */
    private HousingAnnouncementsResponse toResponseFromLh(LhNotice notice) {
        return HousingAnnouncementsResponse.builder()
                .productId(notice.getProduct().getId())
                .id(notice.getId())
                .name(notice.getPanNm())
                .provider("LH 한국토지주택공사")
                .regionName(notice.getCnpCdNm())
                .noticeDate(parseDate(notice.getPanNtStDt()))
                .closeDate(parseDate(notice.getClsgDt()))
                .status(mapStatus(notice.getPanSs()))
                .category(mapCategory(notice.getUppAisTpNm()))
                .detailUrl(notice.getDtlUrl())
                .build();
    }

    /**
     * 날짜 문자열("yyyy.MM.dd" or "yyyy-MM-dd") → LocalDate 변환
     */
    private LocalDate parseDate(String text) {
        try {
            if (text == null || text.isBlank()) return null;
            String normalized = text.replace("-", ".").trim();
            return LocalDate.parse(normalized, FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 안전한 Pageable 객체 생성 및 정렬 필드 매핑
     * - 프론트에서 전달한 정렬 키 → 실제 DB 컬럼으로 변환
     */
    private Pageable sanitizePageable(Pageable pageable, String defaultSort, Sort.Direction direction) {
        if (pageable == null)
            return PageRequest.of(0, 10, Sort.by(direction, defaultSort));

        Sort mappedSort = Sort.by(
                pageable.getSort().stream()
                        .map(order -> {
                            String property = order.getProperty();
                            Sort.Direction dir = order.getDirection();

                            return switch (property) {
                                case "noticeDate" -> new Sort.Order(dir, "panNtStDt");
                                case "closeDate" -> new Sort.Order(dir, "clsgDt");
                                case "regionName" -> new Sort.Order(dir, "cnpCdNm");
                                case "status" -> new Sort.Order(dir, "panSs");
                                default -> new Sort.Order(dir, defaultSort);
                            };
                        })
                        .toList()
        );

        if (mappedSort.isUnsorted() || !mappedSort.iterator().hasNext()) {
            mappedSort = Sort.by(direction, defaultSort);
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
    }

    private Page<HousingAnnouncementsResponse> toPage(List<HousingAnnouncementsResponse> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<HousingAnnouncementsResponse> content = list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }
}
