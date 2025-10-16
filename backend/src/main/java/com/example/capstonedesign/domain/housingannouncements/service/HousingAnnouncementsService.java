package com.example.capstonedesign.domain.housingannouncements.service;

import com.example.capstonedesign.domain.housingannouncements.dto.response.HousingAnnouncementsResponse;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingAnnouncements;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingStatus;
import com.example.capstonedesign.domain.housingannouncements.repository.HousingAnnouncementsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

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
    // 내부 유틸리티
    // --------------------------------------------------------

    /**
     * Entity → DTO 변환 메서드
     * (응답 시 Product 엔티티의 연관 필드 포함)
     *
     * @param ha HousingAnnouncements 엔티티
     * @return HousingAnnouncementsResponse DTO
     */
    private HousingAnnouncementsResponse toResponse(HousingAnnouncements ha) {
        return new HousingAnnouncementsResponse(
                ha.getId(),
                ha.getProduct().getName(),
                ha.getProduct().getProvider(),
                ha.getRegionName(),
                ha.getNoticeDate(),
                ha.getCloseDate(),
                ha.getStatus(),
                ha.getCategory(),
                ha.getProduct().getDetailUrl()
        );
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
