package com.example.capstonedesign.application.ingest;

import com.example.capstonedesign.domain.finance.financecompanies.entity.FinanceCompanies;
import com.example.capstonedesign.domain.finance.financecompanies.repository.FinanceCompaniesRepository;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceProductsRepository;
import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import com.example.capstonedesign.infra.finlife.FinlifeClient;
import com.example.capstonedesign.infra.finlife.dto.FinlifeCompanySearchResponse;
import com.example.capstonedesign.infra.finlife.dto.FinlifeProductResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FinlifeIngestService {

    private static final String[] GROUPS = {"020000","030300"}; // 020000=은행, 030300=저축은행 등
    private final FinlifeClient client;
    private final FinanceCompaniesRepository companiesRepository;
    private final FinanceProductsRepository financeProductsRepository;
    private final ProductsRepository productsRepository;

    // -------------------- Companies --------------------

    @Transactional
    public int syncCompanies(int maxPages) {
        int saved = 0;
        for (String grp : GROUPS) {
            for (int page = 1; page <= maxPages; page++) {
                FinlifeCompanySearchResponse res = client.getCompanies(grp, page);
                var list = (res == null || res.getResult() == null) ? null : res.getResult().getBaseList();
                if (list == null || list.isEmpty()) break;

                for (var c : list) {
                    FinanceCompanies entity = companiesRepository.findByFinCoNo(c.getFinCoNo())
                            .orElse(FinanceCompanies.builder().finCoNo(c.getFinCoNo()).build());
                    entity.setName(c.getName());
                    entity.setHomepage(c.getHomepage());
                    entity.setContact(c.getContact());
                    companiesRepository.save(entity);   // ✅ 실제 저장
                    saved++;
                }
            }
        }
        return saved;
    }

    // -------------------- Products (Deposit/Saving) --------------------

    @Transactional
    public int syncDepositAndSaving(int maxPages) {
        return syncProductType(FinanceProductType.DEPOSIT, maxPages)
                + syncProductType(FinanceProductType.SAVING,  maxPages);
    }

    private int syncProductType(FinanceProductType type, int maxPages) {
        int saved = 0;
        for (String grp : GROUPS) {
            for (int page = 1; page <= maxPages; page++) {
                FinlifeProductResponse res = (type == FinanceProductType.DEPOSIT)
                        ? client.getDepositProducts(grp, page)
                        : client.getSavingProducts(grp, page);

                var bases = (res == null || res.getResult() == null) ? null : res.getResult().getBaseList();
                if (bases == null || bases.isEmpty()) break;

                for (var base : bases) {
                    // 1) 상위 Products upsert
                    Products prod = productsRepository
                            .findByTypeAndNameAndProvider(ProductType.FINANCE, base.getFinPrdtNm(), base.getCompanyName())
                            .orElse(Products.builder()
                                    .type(ProductType.FINANCE)
                                    .name(base.getFinPrdtNm())
                                    .provider(base.getCompanyName())
                                    .build());
                    prod.setDetail_url(base.getDetailUrl());
                    prod = productsRepository.save(prod);   // ✅ 저장

                    // 2) 하위 FinanceProducts upsert
                    FinanceProducts fp = financeProductsRepository.findByProductAndFinCoNo(prod, base.getFinCoNo())
                            .orElse(FinanceProducts.builder()
                                    .product(prod)
                                    .finCoNo(base.getFinCoNo())
                                    .productType(type)
                                    .build());

                    // 가입 조건 텍스트 결합
                    String joinCond = String.join("\n",
                            safeLine("가입 방법: ", base.getJoinWay()),
                            safeLine("가입 대상: ", base.getJoinMember()),
                            safeLine("비고: ", base.getEtcNote())
                    ).trim();
                    fp.setJoin_condition(joinCond.isBlank() ? null : joinCond);

                    // 대표 금리(옵션에서 최고 우대금리 → 없으면 기본금리)
                    BigDecimal rate = extractRepresentativeRate(res, base.getFinPrdtCd(), base.getFinCoNo());
                    fp.setInterest_rate(rate);

                    // 최소 예치금은 스펙 없으면 null 유지
                    fp.setMin_deposit(null);

                    financeProductsRepository.save(fp);     // ✅ 저장
                    saved++;
                }
            }
        }
        return saved;
    }

    private static String safeLine(String label, String val) {
        return (val == null || val.isBlank()) ? "" : (label + val);
    }

    private static BigDecimal extractRepresentativeRate(FinlifeProductResponse res, String prdtCd, String finCoNo) {
        var options = (res.getResult() == null) ? null : res.getResult().getOptionList();
        if (options == null) return null;
        return options.stream()
                .filter(o -> prdtCd.equals(o.getFinPrdtCd()) && finCoNo.equals(o.getFinCoNo()))
                .map(o -> o.getInterestRateMax() != null ? o.getInterestRateMax() : o.getInterestRate())
                .filter(r -> r != null)
                .max(BigDecimal::compareTo)
                .orElse(null);
    }
}
