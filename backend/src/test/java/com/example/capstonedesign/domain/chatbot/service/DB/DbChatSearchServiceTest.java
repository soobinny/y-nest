package com.example.capstonedesign.domain.chatbot.service.DB;

import com.example.capstonedesign.domain.housingannouncements.entity.LhNotice;
import com.example.capstonedesign.domain.housingannouncements.repository.LhNoticeRepository;
import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import com.example.capstonedesign.domain.shannouncements.entity.ShAnnouncement;
import com.example.capstonedesign.domain.shannouncements.repository.ShAnnouncementRepository;
import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import com.example.capstonedesign.domain.youthpolicies.repository.YouthPolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DbChatSearchServiceTest {

    @Mock
    private LhNoticeRepository lhNoticeRepository;

    @Mock
    private ShAnnouncementRepository shAnnouncementRepository;

    @Mock
    private YouthPolicyRepository youthPolicyRepository;

    @Mock
    private ProductsRepository productsRepository;

    @InjectMocks
    private DbChatSearchService dbChatSearchService;

    // ==========================
    // LH 공고 조회 테스트
    // ==========================

    @Test
    void findTopLhByRegionAndKeyword_whenKeywordBlank_usesRegionOnlyQuery() {
        // given
        LhNotice lh = mock(LhNotice.class);
        when(lhNoticeRepository.findTop5ByCnpCdNmContainingOrderByClsgDtAsc(""))
                .thenReturn(List.of(lh));

        // when: region = "전체" → regionLike = ""
        List<LhNotice> result = dbChatSearchService.findTopLhByRegionAndKeyword("전체", "", 5);

        // then
        assertThat(result).containsExactly(lh);
        verify(lhNoticeRepository).findTop5ByCnpCdNmContainingOrderByClsgDtAsc("");
        verify(lhNoticeRepository, never())
                .findTop5ByCnpCdNmContainingAndPanNmContainingOrderByClsgDtAsc(anyString(), anyString());
    }

    @Test
    void findTopLhByRegionAndKeyword_whenKeywordPresent_usesRegionAndTitleQuery() {
        // given
        LhNotice lh = mock(LhNotice.class);
        when(lhNoticeRepository
                .findTop5ByCnpCdNmContainingAndPanNmContainingOrderByClsgDtAsc("서울", "전세"))
                .thenReturn(List.of(lh));

        // when
        List<LhNotice> result = dbChatSearchService.findTopLhByRegionAndKeyword("서울", "전세", 5);

        // then
        assertThat(result).containsExactly(lh);
        verify(lhNoticeRepository)
                .findTop5ByCnpCdNmContainingAndPanNmContainingOrderByClsgDtAsc("서울", "전세");
        verify(lhNoticeRepository, never())
                .findTop5ByCnpCdNmContainingOrderByClsgDtAsc(anyString());
    }

    // ==========================
    // SH 공고 조회 테스트
    // ==========================

    @Test
    void findTopShByRegionAndKeyword_whenKeywordBlank_usesRegionOnlyQuery() {
        // given
        ShAnnouncement sh = mock(ShAnnouncement.class);
        when(shAnnouncementRepository.findTop5ByRegionContainingOrderByPostDateAsc(""))
                .thenReturn(List.of(sh));

        // when: region = "전체" → regionLike = "", keyword = "" → blank
        List<ShAnnouncement> result = dbChatSearchService.findTopShByRegionAndKeyword("전체", "", 5);

        // then
        assertThat(result).containsExactly(sh);
        verify(shAnnouncementRepository).findTop5ByRegionContainingOrderByPostDateAsc("");
        verify(shAnnouncementRepository, never())
                .findTop5ByRegionContainingAndTitleContainingOrderByPostDateAsc(anyString(), anyString());
    }

    @Test
    void findTopShByRegionAndKeyword_whenKeywordPresent_usesRegionAndTitleQuery() {
        // given
        ShAnnouncement sh = mock(ShAnnouncement.class);
        when(shAnnouncementRepository
                .findTop5ByRegionContainingAndTitleContainingOrderByPostDateAsc("부산", "청년"))
                .thenReturn(List.of(sh));

        // when
        List<ShAnnouncement> result = dbChatSearchService.findTopShByRegionAndKeyword("부산", "청년", 5);

        // then
        assertThat(result).containsExactly(sh);
        verify(shAnnouncementRepository)
                .findTop5ByRegionContainingAndTitleContainingOrderByPostDateAsc("부산", "청년");
        verify(shAnnouncementRepository, never())
                .findTop5ByRegionContainingOrderByPostDateAsc(anyString());
    }

    // ==========================
    // 금융 상품 조회 테스트
    // ==========================

    @Test
    void findTopFinanceByKeyword_whenKeywordNullOrBlank_usesEmptyKeyword() {
        // given
        Products p = mock(Products.class);
        when(productsRepository
                .findTop5ByTypeAndNameContainingIgnoreCaseOrderByIdAsc(ProductType.FINANCE, ""))
                .thenReturn(List.of(p));

        // when
        List<Products> resultNull = dbChatSearchService.findTopFinanceByKeyword(null, 5);
        List<Products> resultBlank = dbChatSearchService.findTopFinanceByKeyword("   ", 5);

        // then
        assertThat(resultNull).containsExactly(p);
        assertThat(resultBlank).containsExactly(p);

        verify(productsRepository, times(2))
                .findTop5ByTypeAndNameContainingIgnoreCaseOrderByIdAsc(ProductType.FINANCE, "");
    }

    @Test
    void findTopFinanceByKeyword_whenKeywordPresent_usesKeywordFilter() {
        // given
        Products p = mock(Products.class);
        when(productsRepository
                .findTop5ByTypeAndNameContainingIgnoreCaseOrderByIdAsc(ProductType.FINANCE, "청년"))
                .thenReturn(List.of(p));

        // when
        List<Products> result = dbChatSearchService.findTopFinanceByKeyword("청년", 5);

        // then
        assertThat(result).containsExactly(p);
        verify(productsRepository)
                .findTop5ByTypeAndNameContainingIgnoreCaseOrderByIdAsc(ProductType.FINANCE, "청년");
    }

    // ==========================
    // 청년 정책 조회 테스트
    // ==========================

    @Test
    void findTopPolicyByKeyword_whenKeywordNull_usesEmptyKeywordForBothFields() {
        // given
        YouthPolicy policy = mock(YouthPolicy.class);
        when(youthPolicyRepository
                .findTop5ByPolicyNameContainingOrDescriptionContainingOrderByIdAsc("", ""))
                .thenReturn(List.of(policy));

        // when
        List<YouthPolicy> result = dbChatSearchService.findTopPolicyByKeyword(null, 5);

        // then
        assertThat(result).containsExactly(policy);
        verify(youthPolicyRepository)
                .findTop5ByPolicyNameContainingOrDescriptionContainingOrderByIdAsc("", "");
    }

    @Test
    void findTopPolicyByKeyword_whenKeywordPresent_usesSameKeywordForNameAndDescription() {
        // given
        YouthPolicy policy = mock(YouthPolicy.class);
        when(youthPolicyRepository
                .findTop5ByPolicyNameContainingOrDescriptionContainingOrderByIdAsc("교통", "교통"))
                .thenReturn(List.of(policy));

        // when
        List<YouthPolicy> result = dbChatSearchService.findTopPolicyByKeyword("교통", 5);

        // then
        assertThat(result).containsExactly(policy);
        verify(youthPolicyRepository)
                .findTop5ByPolicyNameContainingOrDescriptionContainingOrderByIdAsc("교통", "교통");
    }
}
