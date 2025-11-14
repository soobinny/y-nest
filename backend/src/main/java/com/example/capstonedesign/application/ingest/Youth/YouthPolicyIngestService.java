package com.example.capstonedesign.application.ingest.Youth;

import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import com.example.capstonedesign.domain.youthpolicies.dto.response.YouthPolicyApiResponse;
import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import com.example.capstonedesign.domain.youthpolicies.repository.YouthPolicyRepository;
import com.example.capstonedesign.infra.youth.YouthPolicyClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * YouthPolicyIngestService
 * -------------------------------------------------
 * - 온통청년(Youth Center) 정책 데이터 수집 서비스
 * - API 호출을 통해 모든 페이지의 정책 데이터를 가져와 DB에 저장
 * - 중복 정책(plcyNo)은 무시하고 신규 데이터만 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YouthPolicyIngestService {

    private final YouthPolicyClient client;
    private final YouthPolicyRepository repository;
    private final ProductsRepository productsRepository;

    /**
     * 온통청년 정책 전체 수집
     * -------------------------------------------------
     * - 페이지 단위로 반복 호출
     * - 중복 정책은 스킵하고 신규 정책만 저장
     */
    @Transactional
    public void ingestAllPolicies() {
        int page = 1;
        int size = 100;

        while (true) {
            YouthPolicyApiResponse response = client.fetchPolicies(page, size, "", "");

            if (response == null ||
                    response.getResult() == null ||
                    response.getResult().getYouthPolicyList() == null ||
                    response.getResult().getYouthPolicyList().isEmpty()) {
                log.info("📭 더 이상 데이터 없음 (page={})", page);
                break;
            }

            response.getResult().getYouthPolicyList().forEach(item -> {

                repository.findByPolicyNo(item.getPlcyNo()).ifPresentOrElse(
                        existing -> log.debug("✅ 이미 존재: {}", item.getPlcyNo()),
                        () -> {
                            // ============================
                            // 1) Products 생성
                            // ============================
                            Products product = productsRepository.save(
                                    Products.builder()
                                            .type(ProductType.POLICY)
                                            .name(item.getPlcyNm())
                                            .provider(item.getSprvsnInstCdNm())
                                            .detailUrl(item.getAplyUrlAddr())
                                            .build()
                            );

                            // ============================
                            // 2) YouthPolicy 생성 + FK 연결
                            // ============================
                            YouthPolicy policy = YouthPolicy.builder()
                                    .product(product)
                                    .policyNo(item.getPlcyNo())
                                    .policyName(item.getPlcyNm())
                                    .description(item.getPlcyExplnCn())
                                    .keyword(item.getPlcyKywdNm())
                                    .categoryLarge(item.getLclsfNm())
                                    .categoryMiddle(item.getMclsfNm())
                                    .agency(item.getSprvsnInstCdNm())
                                    .applyUrl(item.getAplyUrlAddr())
                                    .regionCode(item.getZipCd())
                                    .targetAge(item.getSprtTrgtMinAge() + " ~ " + item.getSprtTrgtMaxAge())
                                    .supportContent(item.getPlcySprtCn())
                                    .startDate(item.getBizPrdBgngYmd())
                                    .endDate(item.getBizPrdEndYmd())
                                    .build();

                            repository.save(policy);

                            log.info("🆕 신규 정책 저장: {} ({})",
                                    item.getPlcyNm(), item.getPlcyNo());
                        }
                );
            });

            log.info("📦 {}건 수집 완료 (page={})",
                    response.getResult().getYouthPolicyList().size(), page);

            page++;
        }
    }

    /** 프로젝트 전체 일관성을 위한 Wrapper */
    public void syncPolicies() {
        ingestAllPolicies();
    }
}
