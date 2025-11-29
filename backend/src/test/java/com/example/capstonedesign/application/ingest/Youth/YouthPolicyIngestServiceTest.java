package com.example.capstonedesign.application.ingest.Youth;

import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import com.example.capstonedesign.domain.youthpolicies.dto.response.YouthPolicyApiResponse;
import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import com.example.capstonedesign.domain.youthpolicies.repository.YouthPolicyRepository;
import com.example.capstonedesign.infra.youth.YouthPolicyClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * YouthPolicyIngestService 단위 테스트
 * - 정책 전체 수집 로직(페이지 루프 + 중복/신규 분기) 검증
 * - syncPolicies() 래퍼 메서드 검증
 */
@ExtendWith(MockitoExtension.class)
class YouthPolicyIngestServiceTest {

    @Mock
    YouthPolicyClient client;

    @Mock
    YouthPolicyRepository repository;

    @Mock
    ProductsRepository productsRepository;

    @InjectMocks
    YouthPolicyIngestService service;

    /**
     * 편의상 Result + PolicyItem을 직접 만들어 주는 헬퍼
     */
    private YouthPolicyApiResponse createPageResponse(List<YouthPolicyApiResponse.PolicyItem> items) {
        YouthPolicyApiResponse.Result result = new YouthPolicyApiResponse.Result();
        result.setYouthPolicyList(items);

        YouthPolicyApiResponse resp = new YouthPolicyApiResponse();
        resp.setResult(result);
        return resp;
    }

    private YouthPolicyApiResponse.PolicyItem createItem(
            String no, String name, String agency, String url
    ) {
        YouthPolicyApiResponse.PolicyItem item = new YouthPolicyApiResponse.PolicyItem();
        item.setPlcyNo(no);
        item.setPlcyNm(name);
        item.setSprvsnInstCdNm(agency);
        item.setAplyUrlAddr(url);

        item.setPlcyExplnCn("정책 설명");
        item.setPlcyKywdNm("키워드");
        item.setLclsfNm("대분류");
        item.setMclsfNm("중분류");
        item.setZipCd("00000");
        item.setSprtTrgtMinAge("19");
        item.setSprtTrgtMaxAge("34");
        item.setPlcySprtCn("지원 내용");
        item.setBizPrdBgngYmd("20240101");
        item.setBizPrdEndYmd("20241231");

        return item;
    }

    // ---------------------------------------------------------------------
    // 1. 정상 플로우: 1페이지에 기존 + 신규 / 2페이지는 빈 리스트 → 루프 종료
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("ingestAllPolicies() - 기존 정책은 스킵, 신규 정책만 저장한다")
    void ingestAllPolicies_savesOnlyNewPolicies() {
        // 1페이지: existing + new 두 개
        YouthPolicyApiResponse.PolicyItem existingItem =
                createItem("P001", "기존 정책", "서울시", "https://apply/existing");
        YouthPolicyApiResponse.PolicyItem newItem =
                createItem("P002", "신규 정책", "서울시", "https://apply/new");

        YouthPolicyApiResponse page1 = createPageResponse(List.of(existingItem, newItem));

        // 2페이지: 빈 리스트 → 종료 조건
        YouthPolicyApiResponse page2 = createPageResponse(List.of());

        when(client.fetchPolicies(1, 100, "", "")).thenReturn(page1);
        when(client.fetchPolicies(2, 100, "", "")).thenReturn(page2);

        // P001은 이미 존재 → Optional.of(...)
        YouthPolicy existingPolicy = YouthPolicy.builder()
                .policyNo("P001")
                .policyName("기존 정책")
                .build();

        when(repository.findByPolicyNo("P001"))
                .thenReturn(Optional.of(existingPolicy));

        // P002는 신규 → Optional.empty(...)
        when(repository.findByPolicyNo("P002"))
                .thenReturn(Optional.empty());

        // Products / YouthPolicy 저장 시 넘겨받은 객체 그대로 반환
        when(productsRepository.save(any(Products.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(YouthPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        service.ingestAllPolicies();

        // then
        verify(client, times(1)).fetchPolicies(1, 100, "", "");
        verify(client, times(1)).fetchPolicies(2, 100, "", "");

        verify(repository, times(1)).findByPolicyNo("P001");
        verify(repository, times(1)).findByPolicyNo("P002");

        // 기존(P001)은 save 호출 안 되고, 신규(P002)만 저장
        ArgumentCaptor<Products> productCaptor = ArgumentCaptor.forClass(Products.class);
        ArgumentCaptor<YouthPolicy> policyCaptor = ArgumentCaptor.forClass(YouthPolicy.class);

        verify(productsRepository, times(1)).save(productCaptor.capture());
        verify(repository, times(1)).save(policyCaptor.capture());

        Products savedProduct = productCaptor.getValue();
        YouthPolicy savedPolicy = policyCaptor.getValue();

        assertThat(savedProduct.getType()).isEqualTo(ProductType.POLICY);
        assertThat(savedProduct.getName()).isEqualTo("신규 정책");
        assertThat(savedProduct.getProvider()).isEqualTo("서울시");
        assertThat(savedProduct.getDetailUrl()).isEqualTo("https://apply/new");

        assertThat(savedPolicy.getPolicyNo()).isEqualTo("P002");
        assertThat(savedPolicy.getPolicyName()).isEqualTo("신규 정책");
        assertThat(savedPolicy.getProduct()).isEqualTo(savedProduct);
    }

    // ---------------------------------------------------------------------
    // 2. 첫 페이지부터 null이면 바로 종료 (while 루프 break 조건 커버)
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("ingestAllPolicies() - 첫 페이지 응답이 null이면 아무 것도 저장하지 않는다")
    void ingestAllPolicies_stopsWhenResponseIsNull() {
        when(client.fetchPolicies(1, 100, "", ""))
                .thenReturn(null);

        assertDoesNotThrow(() -> service.ingestAllPolicies());

        verify(repository, never()).save(any());
        verify(productsRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // 3. syncPolicies() → ingestAllPolicies() 래핑
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("syncPolicies()는 ingestAllPolicies()를 단순 래핑한다")
    void syncPolicies_delegatesToIngestAllPolicies() {
        YouthPolicyIngestService spyService =
                Mockito.spy(new YouthPolicyIngestService(client, repository, productsRepository));

        doNothing().when(spyService).ingestAllPolicies();

        // when
        spyService.syncPolicies();

        // then
        verify(spyService, times(1)).ingestAllPolicies();
    }
}
