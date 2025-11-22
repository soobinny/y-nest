package com.example.capstonedesign.domain.shannouncements.service;

import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.shannouncements.dto.response.ShAnnouncementResponse;
import com.example.capstonedesign.domain.shannouncements.entity.RecruitStatus;
import com.example.capstonedesign.domain.shannouncements.entity.SHHousingCategory;
import com.example.capstonedesign.domain.shannouncements.entity.ShAnnouncement;
import com.example.capstonedesign.domain.shannouncements.repository.ShAnnouncementRepository;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * ShAnnouncementServiceTest
 * -----------------------------------------------------
 * - SH공사 주거 공고 서비스 레이어 단위 테스트
 * - Repository / UsersRepository 를 Mock 으로 대체
 * - 엔티티 → DTO 매핑 및 추천 로직 검증
 */
@ExtendWith(MockitoExtension.class)
class ShAnnouncementServiceTest {

    @Mock
    private ShAnnouncementRepository repo;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private ShAnnouncementService service;

    // ----------------------------------------------------
    // 공통 샘플 엔티티 생성
    // ----------------------------------------------------
    private ShAnnouncement createAnnouncement(
            Long id,
            String title,
            String region,
            String supplyType,
            RecruitStatus status,
            LocalDate postDate
    ) {
        // 1) 연관 Products 더미 객체 생성
        Products product = Products.builder()
                .id(1)                         // Integer/Long 타입은 엔티티에 맞게 사용
                .type(ProductType.HOUSING)
                .name("SH 공고 상품")
                .provider("SH공사")
                .detailUrl("https://example.com/sh/" + id)
                .build();

        // 2) ShAnnouncement 에 product 및 기타 필드 세팅
        return ShAnnouncement.builder()
                .id(id)
                .product(product)
                .title(title)
                .department("청약부")
                .postDate(postDate)
                .views(123)
                .recruitStatus(status)
                .supplyType(supplyType)
                .category(SHHousingCategory.주택임대)  // 임대 카테고리 예시
                .region(region)
                .detailUrl("https://example.com/sh/" + id)
                .build();
    }

    // ----------------------------------------------------
    // [1] 전체 공고 조회 테스트
    // ----------------------------------------------------
    @Test
    @DisplayName("getAll - 전체 공고가 DTO 페이지로 정상 매핑된다")
    void getAll_shouldReturnPageOfResponses() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        ShAnnouncement a1 = createAnnouncement(
                1L,
                "청년안심주택 A단지 모집공고",
                "서울 강남구",
                "청년안심주택",
                RecruitStatus.now,
                LocalDate.now()
        );

        Page<ShAnnouncement> page = new PageImpl<>(List.of(a1), pageable, 1);
        given(repo.findAll(any(Pageable.class))).willReturn(page);

        // when
        Page<ShAnnouncementResponse> result = service.getAll(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        ShAnnouncementResponse dto = result.getContent().get(0);
        assertThat(dto.getTitle()).isEqualTo("청년안심주택 A단지 모집공고");
        assertThat(dto.getRecruitStatus()).isEqualTo("now");
        assertThat(dto.getProductId()).isNotNull();
    }

    // ----------------------------------------------------
    // [2] 조건 검색 테스트 (간단 케이스)
    // ----------------------------------------------------
    @Test
    @DisplayName("search - 키워드 기반 검색 시 DTO 페이지를 반환한다")
    void search_shouldReturnPageOfResponses() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        ShAnnouncement a1 = createAnnouncement(
                1L,
                "청년안심주택 A단지 모집공고",
                "서울 강남구",
                "청년안심주택",
                RecruitStatus.now,
                LocalDate.now()
        );

        Page<ShAnnouncement> page = new PageImpl<>(List.of(a1), pageable, 1);

        // Specification + Pageable 조합
        given(repo.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(page);

        // when
        Page<ShAnnouncementResponse> result = service.search(
                null,         // category
                null,         // status
                "청년",        // keyword
                pageable
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle())
                .contains("청년안심주택");
    }

    // ----------------------------------------------------
    // [3] 최근 공고 조회 테스트
    // ----------------------------------------------------
    @Test
    @DisplayName("getRecent - 최근 7일 내 공고만 DTO 페이지로 반환한다 (Repository 결과 기준)")
    void getRecent_shouldReturnRecentAnnouncements() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        ShAnnouncement a1 = createAnnouncement(
                1L,
                "최근 등록된 청년안심주택",
                "서울 강남구",
                "청년안심주택",
                RecruitStatus.now,
                LocalDate.now().minusDays(2)
        );

        Page<ShAnnouncement> page = new PageImpl<>(List.of(a1), pageable, 1);
        given(repo.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(page);

        // when
        Page<ShAnnouncementResponse> result = service.getRecent(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle())
                .isEqualTo("최근 등록된 청년안심주택");
    }

    // ----------------------------------------------------
    // [4] 청년 친화형 공고 추천 (지역 필터) 테스트
    // ----------------------------------------------------
    @Test
    @DisplayName("getYouthRecommendations(region) - 지역 필터 포함 청년형 공고 추천")
    void getYouthRecommendations_withRegion_shouldReturnPageOfResponses() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        ShAnnouncement a1 = createAnnouncement(
                1L,
                "강남 청년안심주택 모집공고",
                "서울 강남구",
                "청년안심주택",
                RecruitStatus.now,
                LocalDate.now()
        );

        Page<ShAnnouncement> page = new PageImpl<>(List.of(a1), pageable, 1);
        given(repo.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(page);

        // when
        Page<ShAnnouncementResponse> result =
                service.getYouthRecommendations("강남", pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        ShAnnouncementResponse dto = result.getContent().get(0);
        assertThat(dto.getTitle()).contains("강남");
        assertThat(dto.getSupplyType()).isEqualTo("청년안심주택");
    }

    // ----------------------------------------------------
    // [5] 사용자 맞춤 추천 테스트
    // ----------------------------------------------------
    @Test
    @DisplayName("recommendForUser - 나이/소득/지역에 맞는 SH 공고만 필터링하여 점수순으로 추천한다")
    void recommendForUser_shouldFilterAndSortByScore() {
        // given
        Users user = Users.builder()
                .id(1)
                .email("test@example.com")
                .password("encoded")
                .name("테스터")
                .age(28)
                .income_band("중위소득 150% 이하")
                .region("서울")
                .is_homeless(true)
                .build();

        given(usersRepository.findById(1))
                .willReturn(Optional.of(user));

        ShAnnouncement youthSeoul = createAnnouncement(
                1L,
                "서울 청년안심주택 A단지",
                "서울 강남구",
                "청년안심주택",
                RecruitStatus.now,
                LocalDate.now().minusDays(1)
        );

        ShAnnouncement youthBusan = createAnnouncement(
                2L,
                "부산 청년안심주택 B단지",
                "부산 남구",
                "청년안심주택",
                RecruitStatus.now,
                LocalDate.now().minusDays(2)
        );

        ShAnnouncement saleHighIncome = createAnnouncement(
                3L,
                "분양형 일반주택 C단지",
                "서울 송파구",
                "분양주택",
                RecruitStatus.now,
                LocalDate.now().minusDays(5)
        );

        given(repo.findAll())
                .willReturn(List.of(youthSeoul, youthBusan, saleHighIncome));

        // when
        List<ShAnnouncementResponse> result = service.recommendForUser(1, false);

        // then
        // 1) 지역 필터: 서울 거주 → 서울 공고만 남는다.
        assertThat(result)
                .hasSize(1)
                .extracting(ShAnnouncementResponse::getTitle)
                .containsExactly("서울 청년안심주택 A단지");

        assertThat(result)
                .extracting(ShAnnouncementResponse::getTitle)
                .doesNotContain("부산 청년안심주택 B단지", "분양형 일반주택 C단지");

        // 2) score / reason 기본 확인
        ShAnnouncementResponse first = result.get(0);
        assertThat(first.getScore()).isNotNull();
        assertThat(first.getReason()).isNotBlank();
    }
}
