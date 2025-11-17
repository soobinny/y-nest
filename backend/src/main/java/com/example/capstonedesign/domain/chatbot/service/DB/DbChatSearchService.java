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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DbChatSearchService {

    private final LhNoticeRepository lhNoticeRepository;
    private final ShAnnouncementRepository shAnnouncementRepository;
    private final YouthPolicyRepository youthPolicyRepository;
    private final ProductsRepository productsRepository;

    // ====== 주거 공고 조회 ======
    public List<LhNotice> findTopLhByRegionAndKeyword(String region, String keyword, int limit) {
        String regionLike = "전체".equals(region) ? "" : region;
        String keywordLike = (keyword == null ? "" : keyword);

        // 키워드가 없으면: 지역 기준 전체 공고 조회
        if (keywordLike.isBlank()) {
            return lhNoticeRepository
                    .findTop5ByCnpCdNmContainingOrderByClsgDtAsc(regionLike);
        }

        // 키워드가 있으면: 지역 + 공고명으로 필터
        return lhNoticeRepository
                .findTop5ByCnpCdNmContainingAndPanNmContainingOrderByClsgDtAsc(
                        regionLike,
                        keywordLike
                );
    }

    public List<ShAnnouncement> findTopShByRegionAndKeyword(String region, String keyword, int limit) {
        String regionLike = "전체".equals(region) ? "" : region;
        String keywordLike = (keyword == null ? "" : keyword);

        // 키워드가 없으면: 지역 기준 전체 공고 조회
        if (keywordLike.isBlank()) {
            return shAnnouncementRepository
                    .findTop5ByRegionContainingOrderByPostDateAsc(regionLike);
        }

        // 키워드가 있으면: 지역 + 제목으로 필터
        return shAnnouncementRepository
                .findTop5ByRegionContainingAndTitleContainingOrderByPostDateAsc(
                        regionLike,
                        keywordLike
                );
    }

    // ====== 금융 상품 조회 ======
    public List<Products> findTopFinanceByKeyword(String keyword, int limit) {
        String keywordLike = (keyword == null || keyword.isBlank()) ? "" : keyword;

        return productsRepository
                .findTop5ByTypeAndNameContainingIgnoreCaseOrderByIdAsc(
                        ProductType.FINANCE,
                        keywordLike
                );
    }

    // ====== 청년 정책 조회 ======
    public List<YouthPolicy> findTopPolicyByKeyword(String keyword, int limit) {
        String keywordLike = (keyword == null ? "" : keyword);

        return youthPolicyRepository
                .findTop5ByPolicyNameContainingOrDescriptionContainingOrderByIdAsc(
                        keywordLike,
                        keywordLike
                );
    }
}
