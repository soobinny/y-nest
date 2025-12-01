package com.example.capstonedesign.domain.youthpolicies.controller;

import com.example.capstonedesign.application.ingest.Youth.YouthPolicyIngestService;
import com.example.capstonedesign.domain.youthpolicies.dto.response.YouthPolicyResponse;
import com.example.capstonedesign.domain.youthpolicies.service.YouthPolicyQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class YouthPolicyControllerPagingSortTest {

    private YouthPolicyQueryService queryService;
    private YouthPolicyIngestService ingestService;
    private YouthPolicyController controller;

    @BeforeEach
    void setUp() {
        queryService = mock(YouthPolicyQueryService.class);
        ingestService = mock(YouthPolicyIngestService.class);
        controller = new YouthPolicyController(queryService, ingestService);

        // 공통 스텁: 어차피 Page 내용이 중요하진 않고, Pageable/Sort 인자만 확인할 거라 대충 빈 페이지
        when(queryService.getPaged(any(), any(), any(Pageable.class), any(Sort.class)))
                .thenReturn(new PageImpl<>(Collections.<YouthPolicyResponse>emptyList()));
    }

    @Test
    @DisplayName("pageable == null 일 때 기본 PageRequest.of(0,10) 과 createdAt DESC 정렬을 사용한다")
    void getPolicies_whenPageableNull_usesDefaultPageAndDefaultSort() {
        // given
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        // when
        controller.getPolicies(null, null, null, null); // pageable, sort 둘 다 null

        // then
        verify(queryService).getPaged(
                isNull(), isNull(),
                pageableCaptor.capture(),
                sortCaptor.capture()
        );

        Pageable resolved = pageableCaptor.getValue();
        Sort sort = sortCaptor.getValue();

        // 기본 페이징: page=0, size=10
        assertThat(resolved.getPageNumber()).isEqualTo(0);
        assertThat(resolved.getPageSize()).isEqualTo(10);

        // 기본 정렬: createdAt DESC
        assertThat(sort.isSorted()).isTrue();
        Sort.Order order = sort.getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("sort 파라미터가 없고 pageable 에만 sort 가 있으면 pageable 의 sort 를 사용한다")
    void getPolicies_whenSortParamAbsent_usePageableSort() {
        // given
        Pageable pageableWithSort =
                PageRequest.of(1, 20, Sort.by(Sort.Direction.ASC, "startDate"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        // when
        controller.getPolicies("키워드", "11000", null, pageableWithSort);

        // then
        verify(queryService).getPaged(
                eq("키워드"), eq("11000"),
                pageableCaptor.capture(),
                sortCaptor.capture()
        );

        Pageable resolved = pageableCaptor.getValue();
        Sort sort = sortCaptor.getValue();

        // 페이징은 page=1, size=20 유지
        assertThat(resolved.getPageNumber()).isEqualTo(1);
        assertThat(resolved.getPageSize()).isEqualTo(20);

        // sortSpec.isUnsorted() && pageable.getSort().isSorted() 분기 → pageable.sort 사용
        assertThat(sort.isSorted()).isTrue();
        Sort.Order order = sort.getOrderFor("startDate");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("sort 파라미터가 있으면 resolveSort 결과를 사용하고 pageable.sort 는 무시된다")
    void getPolicies_whenSortParamPresent_useSortParam() {
        // given
        Pageable pageableWithSort =
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "somethingElse"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        // when
        controller.getPolicies(null, null, "startDate,asc", pageableWithSort);

        // then
        verify(queryService).getPaged(
                isNull(), isNull(),
                pageableCaptor.capture(),
                sortCaptor.capture()
        );

        Pageable resolved = pageableCaptor.getValue();
        Sort sort = sortCaptor.getValue();

        // pageable 쪽 page/size는 그대로
        assertThat(resolved.getPageNumber()).isEqualTo(0);
        assertThat(resolved.getPageSize()).isEqualTo(10);

        // sortParam "startDate,asc" → resolveSort()에서 ASC 정렬
        assertThat(sort.isSorted()).isTrue();
        Sort.Order order = sort.getOrderFor("startDate");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }
}
