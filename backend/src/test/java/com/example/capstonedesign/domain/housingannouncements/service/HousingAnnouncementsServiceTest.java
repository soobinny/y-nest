package com.example.capstonedesign.domain.housingannouncements.service;

import com.example.capstonedesign.domain.housingannouncements.dto.response.HousingAnnouncementsResponse;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingStatus;
import com.example.capstonedesign.domain.housingannouncements.entity.LhNotice;
import com.example.capstonedesign.domain.housingannouncements.repository.LhNoticeRepository;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HousingAnnouncementsServiceTest {

    @Mock
    private LhNoticeRepository lhNoticeRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private HousingAnnouncementsService housingAnnouncementsService;

    private Products product;

    @BeforeEach
    void setUp() {
        // Products 엔티티 구조에 맞게 수정해서 사용하면 됨
        product = Products.builder()
                .id(1) // 혹은 Long 타입이면 1L
                .name("LH 행복주택")
                .build();
    }

    private LhNotice createNotice(
            Long id,
            String panNm,
            String uppAisTpNm,
            String aisTpCdNm,
            String cnpCdNm,
            String panSs,
            String panNtStDt,
            String clsgDt
    ) {
        return LhNotice.builder()
                .id(id)
                .product(product)
                .panNm(panNm)
                .uppAisTpNm(uppAisTpNm)
                .aisTpCdNm(aisTpCdNm)
                .cnpCdNm(cnpCdNm)
                .panSs(panSs)
                .panNtStDt(panNtStDt)
                .clsgDt(clsgDt)
                .dtlUrl("https://example.com/" + id)
                .build();
    }

    @Test
    @DisplayName("getAll - 전체 공고를 페이징 형태로 반환한다")
    void getAll_ShouldReturnPagedResponses() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "noticeDate"));
        LhNotice notice = createNotice(
                1L, "행복주택 모집", "임대주택", "행복주택",
                "서울특별시", "공고중", "2025-11-20", "2025-11-30"
        );
        Page<LhNotice> lhPage = new PageImpl<>(List.of(notice), pageable, 1);

        when(lhNoticeRepository.findAll(any(Pageable.class)))
                .thenReturn(lhPage);

        // when
        Page<HousingAnnouncementsResponse> result = housingAnnouncementsService.getAll(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        HousingAnnouncementsResponse res = result.getContent().get(0);
        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getName()).isEqualTo("행복주택 모집");
        assertThat(res.getCategory()).isEqualTo(HousingCategory.임대주택);

        verify(lhNoticeRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("search - 카테고리, 상태, 키워드로 LH 공고 검색")
    void search_ShouldCallRepositoryWithMappedParams() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        LhNotice notice = createNotice(
                1L, "서울 행복주택", "임대주택", "행복주택",
                "서울특별시", "공고중", "2025-11-20", "2025-11-30"
        );
        Page<LhNotice> lhPage = new PageImpl<>(List.of(notice), pageable, 1);

        when(lhNoticeRepository.searchNotices(
                any(), any(), any(), any(Pageable.class)
        )).thenReturn(lhPage);

        // when
        Page<HousingAnnouncementsResponse> result = housingAnnouncementsService.search(
                HousingCategory.임대주택,
                HousingStatus.공고중,
                "서울",
                pageable
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getRegionName()).isEqualTo("서울특별시");

        verify(lhNoticeRepository, times(1))
                .searchNotices(
                        eq("임대주택"),
                        eq("공고중"),
                        eq("서울"),
                        any(Pageable.class)
                );
    }

    @Test
    @DisplayName("getClosingSoon - 오늘 기준 3일 이내 마감 공고만 반환한다")
    void getClosingSoon_ShouldFilterAndSortByCloseDate() {
        // given
        LocalDate today = LocalDate.now();

        LhNotice closeTomorrow = createNotice(
                1L, "내일 마감", "임대주택", "행복주택",
                "서울특별시", "공고중",
                today.minusDays(2).toString(),
                today.plusDays(1).toString()
        );
        LhNotice closeInFiveDays = createNotice(
                2L, "5일 뒤 마감", "임대주택", "행복주택",
                "서울특별시", "공고중",
                today.minusDays(1).toString(),
                today.plusDays(5).toString()
        );

        when(lhNoticeRepository.findAll())
                .thenReturn(List.of(closeTomorrow, closeInFiveDays));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<HousingAnnouncementsResponse> result = housingAnnouncementsService.getClosingSoon(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        HousingAnnouncementsResponse res = result.getContent().get(0);
        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getCloseDate()).isEqualTo(today.plusDays(1));

        verify(lhNoticeRepository).findAll();
    }

    @Test
    @DisplayName("getRecent - 최근 7일 이내 게시된 공고만 반환한다")
    void getRecent_ShouldFilterByNoticeDate() {
        // given
        LocalDate today = LocalDate.now();

        LhNotice recent = createNotice(
                1L, "최근 공고", "임대주택", "행복주택",
                "서울특별시", "공고중",
                today.minusDays(3).toString(), // 최근 7일 이내
                today.plusDays(10).toString()
        );
        LhNotice old = createNotice(
                2L, "오래된 공고", "임대주택", "행복주택",
                "서울특별시", "공고중",
                today.minusDays(20).toString(), // 7일 이전
                today.plusDays(10).toString()
        );

        when(lhNoticeRepository.findAll())
                .thenReturn(List.of(recent, old));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<HousingAnnouncementsResponse> result = housingAnnouncementsService.getRecent(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        HousingAnnouncementsResponse res = result.getContent().get(0);
        assertThat(res.getId()).isEqualTo(1L);

        verify(lhNoticeRepository).findAll();
    }

    @Test
    @DisplayName("recommendForUser - 사용자 정보 기반 상위 10개의 추천 공고를 반환한다")
    void recommendForUser_ShouldReturnScoredRecommendations() {
        // given
        Users user = Users.builder()
                .id(1)
                .age(27)
                .region("서울특별시 강남구")
                .income_band("중위소득100%이하")
                .build();

        when(usersRepository.findById(1))
                .thenReturn(Optional.of(user));

        LocalDate today = LocalDate.now();

        LhNotice n1 = createNotice(
                1L, "서울 행복주택 A", "임대주택", "행복주택",
                "서울특별시 강남구", "공고중",
                today.minusDays(1).toString(),
                today.plusDays(3).toString()
        );
        LhNotice n2 = createNotice(
                2L, "부산 행복주택 B", "임대주택", "행복주택",
                "부산광역시", "공고중",
                today.minusDays(2).toString(),
                today.plusDays(5).toString()
        );

        when(lhNoticeRepository.findAll())
                .thenReturn(List.of(n1, n2));

        // when
        var result = housingAnnouncementsService.recommendForUser(1, false);

        // then
        assertThat(result).hasSize(2);
        // score가 낮을수록 우선순위이므로, 서울 공고가 더 앞에 와야 함
        assertThat(result.get(0).getName()).isEqualTo("서울 행복주택 A");
        assertThat(result.get(0).getScore()).isNotNull();
        assertThat(result.get(0).getReason()).isNotBlank();

        verify(usersRepository).findById(1);
        verify(lhNoticeRepository).findAll();
    }

    @Test
    @DisplayName("getAll - pageable이 null이면 기본 page=0,size=10, panNtStDt DESC 정렬을 사용한다")
    void getAll_whenPageableNull_usesDefaultPageableAndSort() {
        // given
        LhNotice notice = createNotice(
                1L, "기본 페이지 공고", "임대주택", "행복주택",
                "서울특별시", "공고중", "2025.11.20", "2025.11.30"
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(lhNoticeRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notice)));

        // when
        Page<HousingAnnouncementsResponse> result = housingAnnouncementsService.getAll(null);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(lhNoticeRepository).findAll(pageableCaptor.capture());
        Pageable usedPageable = pageableCaptor.getValue();

        assertThat(usedPageable.getPageNumber()).isEqualTo(0);
        assertThat(usedPageable.getPageSize()).isEqualTo(10);

        Sort sort = usedPageable.getSort();
        assertThat(sort.isSorted()).isTrue();
        Sort.Order order = sort.getOrderFor("panNtStDt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("getClosingSoon - null 또는 파싱 불가 마감일은 무시된다")
    void getClosingSoon_ignoresNullOrInvalidCloseDate() {
        // given
        LocalDate today = LocalDate.now();

        LhNotice valid = createNotice(
                1L, "유효 마감", "임대주택", "행복주택",
                "서울특별시", "공고중",
                today.minusDays(1).toString(),
                today.plusDays(2).toString()
        );
        LhNotice invalid = createNotice(
                2L, "잘못된 날짜 형식", "임대주택", "행복주택",
                "서울특별시", "공고중",
                today.toString(),
                "2025/11/30" // parse 실패용
        );
        LhNotice nullClose = createNotice(
                3L, "마감일 없음", "임대주택", "행복주택",
                "서울특별시", "공고중",
                today.toString(),
                null
        );

        when(lhNoticeRepository.findAll())
                .thenReturn(List.of(valid, invalid, nullClose));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<HousingAnnouncementsResponse> result = housingAnnouncementsService.getClosingSoon(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("recommendForUser(strictRegionMatch=true) - 같은 광역시이지만 다른 시군구는 가점이 줄어든다")
    void recommendForUser_strictRegionMatch_affectsScore() {
        // given
        Users user = Users.builder()
                .id(1)
                .age(29)
                .region("서울특별시 강남구")
                .income_band("중위소득200%이하")
                .build();

        when(usersRepository.findById(1))
                .thenReturn(Optional.of(user));

        LocalDate today = LocalDate.now();

        LhNotice sameDistrict = createNotice(
                1L, "강남 행복주택", "임대주택", "행복주택",
                "서울특별시 강남구", "공고중",
                today.minusDays(1).toString(),
                today.plusDays(5).toString()
        );
        LhNotice otherDistrict = createNotice(
                2L, "송파 행복주택", "임대주택", "행복주택",
                "서울특별시 송파구", "공고중",
                today.minusDays(1).toString(),
                today.plusDays(5).toString()
        );

        when(lhNoticeRepository.findAll())
                .thenReturn(List.of(sameDistrict, otherDistrict));

        // 느슨한 지역 매칭
        var loose = housingAnnouncementsService.recommendForUser(1, false);
        // 엄격한 지역 매칭
        var strict = housingAnnouncementsService.recommendForUser(1, true);

        double looseScoreOther = loose.stream()
                .filter(r -> r.getId().equals(2L))
                .findFirst()
                .orElseThrow()
                .getScore();

        double strictScoreOther = strict.stream()
                .filter(r -> r.getId().equals(2L))
                .findFirst()
                .orElseThrow()
                .getScore();

        // strict=true에서 같은 광역시지만 다른 시군구는 지역 가점을 못 받으므로 점수가 더 높아야 한다
        assertThat(strictScoreOther).isGreaterThan(looseScoreOther);
    }

    @Test
    @DisplayName("search - keyword가 null/공백이면 safeKeyword는 null로 repository에 전달된다")
    void search_whenKeywordNullOrBlank_passesNullToRepository() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        when(lhNoticeRepository.searchNotices(
                any(), any(), any(), any(Pageable.class)
        )).thenReturn(Page.empty());

        // when
        housingAnnouncementsService.search(null, null, "   ", pageable);

        // then
        verify(lhNoticeRepository).searchNotices(
                isNull(),  // category -> null
                isNull(),  // status -> null
                isNull(),  // safeKeyword -> null
                any(Pageable.class)
        );
    }
}
