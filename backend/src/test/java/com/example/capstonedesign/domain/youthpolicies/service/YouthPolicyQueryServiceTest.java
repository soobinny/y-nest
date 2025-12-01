package com.example.capstonedesign.domain.youthpolicies.service;

import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import com.example.capstonedesign.domain.youthpolicies.dto.response.YouthPolicyResponse;
import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import com.example.capstonedesign.domain.youthpolicies.repository.YouthPolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YouthPolicyQueryServiceTest {

    @Mock
    private YouthPolicyRepository repository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private YouthPolicyQueryService queryService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private YouthPolicy createPolicy(
            Long id,
            String policyName,
            String startDate,
            String endDate,
            String regionCode,
            String targetAge,
            String keyword,
            LocalDateTime createdAt
    ) {
        Products product = mock(Products.class);
        lenient().when(product.getId()).thenReturn(id.intValue());

        return YouthPolicy.builder()
                .id(id)
                .product(product)
                .policyNo("P-" + id)
                .policyName(policyName)
                .categoryLarge("대분류")
                .categoryMiddle("중분류")
                .keyword(keyword)
                .agency("서울청년센터")
                .regionCode(regionCode)
                .startDate(startDate)
                .endDate(endDate)
                .targetAge(targetAge)
                .supportContent("테스트 지원 내용")
                .applyUrl("http://example.com")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    @Test
    @DisplayName("getPaged - 키워드+지역코드 필터링 및 페이징 정상 동작")
    void getPaged_withKeywordAndRegion_filtersAndPaginates() {
        // given
        LocalDateTime now = LocalDateTime.now();
        YouthPolicy p1 = createPolicy(1L, "청년 취업 지원", "2025-01-01", "2025-12-31",
                "11000", "19~34세", "소득,지원", now.minusDays(1));
        YouthPolicy p2 = createPolicy(2L, "청년 창업 지원", "2025-02-01", "2025-12-31",
                "26000", "19~39세", "창업", now.minusDays(2));
        YouthPolicy p3 = createPolicy(3L, "서울 청년 생활비 지원", "2025-03-01", "2025-12-31",
                "11000", "19~29세", "소득,지원", now.minusDays(3));

        when(repository.findAll()).thenReturn(List.of(p1, p2, p3));

        Pageable pageable = PageRequest.of(0, 10);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        // when
        Page<YouthPolicyResponse> result =
                queryService.getPaged("청년", "11000", pageable, sort);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(YouthPolicyResponse::getPolicyName)
                .containsExactly("청년 취업 지원", "서울 청년 생활비 지원");
    }

    @Test
    @DisplayName("getRecentPolicies - 최근 30일 내 정책만 조회")
    void getRecentPolicies_onlyWithin30Days() {
        // given
        LocalDate today = LocalDate.now();
        String within30 = today.minusDays(5).format(FORMATTER);
        String older = today.minusDays(40).format(FORMATTER);

        YouthPolicy recent = createPolicy(
                1L, "최근 정책", within30, "2025-12-31",
                "11000", "19~34세", "지원", LocalDateTime.now().minusDays(3)
        );
        YouthPolicy old = createPolicy(
                2L, "예전 정책", older, "2025-12-31",
                "11000", "19~34세", "지원", LocalDateTime.now().minusDays(50)
        );

        when(repository.findAll()).thenReturn(List.of(recent, old));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<YouthPolicyResponse> page = queryService.getRecentPolicies(pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getPolicyName()).isEqualTo("최근 정책");
    }

    @Test
    @DisplayName("getClosingSoonPolicies - 7일 내 마감되는 정책만 조회")
    void getClosingSoonPolicies_onlyClosingWithin7Days() {
        // given
        LocalDate today = LocalDate.now();
        String in3Days = today.plusDays(3).format(FORMATTER);
        String in10Days = today.plusDays(10).format(FORMATTER);

        YouthPolicy closingSoon = createPolicy(
                1L, "마감 임박 정책", "2025-01-01", in3Days,
                "11000", "19~34세", "지원", LocalDateTime.now().minusDays(1)
        );
        YouthPolicy later = createPolicy(
                2L, "마감 여유 정책", "2025-01-01", in10Days,
                "11000", "19~34세", "지원", LocalDateTime.now().minusDays(1)
        );
        // 상시 공고 케이스
        YouthPolicy ongoing = createPolicy(
                3L, "상시 공고", "2025-01-01", "00000000",
                "11000", "19~34세", "지원", LocalDateTime.now().minusDays(1)
        );

        when(repository.findAll()).thenReturn(List.of(closingSoon, later, ongoing));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<YouthPolicyResponse> page = queryService.getClosingSoonPolicies(pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getPolicyName()).isEqualTo("마감 임박 정책");
    }

    @Test
    @DisplayName("recommendForUser - 사용자 나이/지역/소득 기반 추천 리스트 반환")
    void recommendForUser_basicRecommendation() {
        // given
        Users user = new Users();
        user.setId(1);
        user.setAge(27);
        user.setRegion("서울특별시");
        user.setIncome_band("중위소득 100% 이하");

        when(usersRepository.findById(1))
                .thenReturn(Optional.of(user));

        LocalDateTime now = LocalDateTime.now();

        YouthPolicy p1 = createPolicy(
                1L, "서울 청년 월세 지원", "2025-01-01", "2025-12-31",
                "11000", "19~34세", "소득,지원", now.minusDays(1)
        );
        YouthPolicy p2 = createPolicy(
                2L, "전국 청년 창업 대출", "2025-01-01", "2025-12-31",
                "11000,26000", "19~39세", "대출", now.minusDays(2)
        );

        when(repository.findAll()).thenReturn(List.of(p1, p2));

        // when
        List<YouthPolicyResponse> result =
                queryService.recommendForUser(1, false);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result.size()).isLessThanOrEqualTo(10);
        // 추천 점수 오름차순 정렬인지 간단 체크
        assertThat(result)
                .isSortedAccordingTo((a, b) -> a.getScore().compareTo(b.getScore()));
    }

    @Test
    @DisplayName("getById - 존재하는 정책 ID 조회 시 정상 반환")
    void getById_existingId_returnsResponse() {
        // given
        YouthPolicy policy = createPolicy(
                1L, "테스트 정책", "2025-01-01", "2025-12-31",
                "11000", "19~34세", "지원", LocalDateTime.now()
        );

        when(repository.findById(1L)).thenReturn(Optional.of(policy));

        // when
        YouthPolicyResponse response = queryService.getById(1L);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPolicyName()).isEqualTo("테스트 정책");
        assertThat(response.getProductId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getById - 존재하지 않는 ID 조회 시 EntityNotFoundException 발생")
    void getById_nonExistingId_throwsException() {
        // given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queryService.getById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ID=999");
    }
}
