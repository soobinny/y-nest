package com.example.capstonedesign.application.ingest;

import com.example.capstonedesign.domain.finance.financecompanies.entity.FinanceCompanies;
import com.example.capstonedesign.domain.finance.financecompanies.repository.FinanceCompaniesRepository;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinlifeCreditLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinlifeMortgageLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinlifeRentLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceLoanOption;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceLoanOptionRepository;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceProductsRepository;
import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import com.example.capstonedesign.infra.finlife.FinlifeClient;
import com.example.capstonedesign.infra.finlife.dto.FinlifeCompanySearchResponse;
import com.example.capstonedesign.infra.finlife.dto.FinlifeLoanProductResponse;
import com.example.capstonedesign.infra.finlife.dto.FinlifeProductResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;


/**
 * 금융감독원 Open API(Finlife) 데이터를 주기적으로 수집/동기화하는 서비스
 * <p>
 * 주요 기능:
 *  - 금융회사 목록 저장(syncCompanies)
 *  - 예금/적금 상품 저장(syncDepositAndSaving)
 *  - 대출 상품 및 옵션 저장(syncLoans)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinlifeIngestService {

    // 대상 금융 그룹: 은행(020000), 저축은행(030300)
    private static final String[] GROUPS = {"020000", "030300"};

    private final FinlifeClient client;
    private final FinanceCompaniesRepository companiesRepository;
    private final FinanceProductsRepository financeProductsRepository;
    private final ProductsRepository productsRepository;
    private final FinanceLoanOptionRepository loanOptionRepository;

    /* ==================== 공통 유틸 ==================== */

    /** 금융회사 코드 정규화 (공백/비가시문자 제거) */
    private String normalizeCoNo(String s) {
        return s == null ? null : s.replace("\u00A0", " ").trim();
    }

    /** 문자열 trim 후 빈 문자열일 경우 null 반환 */
    private static String norm(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** finCoNo가 비어 있을 경우 회사명으로 대체 */
    private static String codeOrName(String code, String name) {
        String c = norm(code);
        return (c != null) ? c : norm(name);
    }

    /** 금융회사 존재 보장 (없으면 생성 후 저장) */
    private void ensureCompany(String finCoNoRaw, String name, String homepage, String contact) {
        String finCoNo = normalizeCoNo(finCoNoRaw);
        FinanceCompanies entity = companiesRepository.findByFinCoNo(finCoNo)
                .orElse(FinanceCompanies.builder().finCoNo(finCoNo).build());
        if (name != null && !name.isBlank()) entity.setName(name);
        if (homepage != null && !homepage.isBlank()) entity.setHomepage(homepage);
        if (contact != null && !contact.isBlank()) entity.setContact(contact);
        companiesRepository.save(entity);
    }

    /* ==================== 금융회사 동기화 ==================== */

    /**
     * Finlife API에서 금융회사 목록을 가져와 DB에 저장
     * @param maxPages 각 그룹별 최대 조회 페이지 수
     * @return 저장된 회사 수
     */
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
                    companiesRepository.save(entity);
                    saved++;
                }
            }
        }
        return saved;
    }

    /* -------------------- 예금 / 적금 동기화 -------------------- */

    /** 예금 및 적금 상품 전체 동기화 */
    @Transactional
    public int syncDepositAndSaving(int maxPages) {
        return syncProductType(FinanceProductType.DEPOSIT, maxPages)
                + syncProductType(FinanceProductType.SAVING, maxPages);
    }

    /**
     * 예금/적금 상품 정보 수집 및 저장
     * - Products / FinanceProducts 테이블에 저장
     */
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
                    // 1) Products(상위 상품) upsert
                    Products prod = productsRepository
                            .findByTypeAndNameAndProvider(ProductType.FINANCE, base.getFinPrdtNm(), base.getCompanyName())
                            .orElse(Products.builder()
                                    .type(ProductType.FINANCE)
                                    .name(base.getFinPrdtNm())
                                    .provider(base.getCompanyName())
                                    .build());

                    // 세부 URL (없으면 회사 홈페이지로 대체)
                    String url = base.getDetailUrl();
                    if (url == null || url.isBlank()) {
                        url = companiesRepository.findByFinCoNo(base.getFinCoNo())
                                .map(FinanceCompanies::getHomepage)
                                .orElse(null);
                    }
                    prod.setDetailUrl(url);
                    productsRepository.save(prod);

                    // 2) FinanceProducts(하위 상품) upsert
                    FinanceProducts fp = financeProductsRepository.findByProductAndFinCoNo(prod, base.getFinCoNo())
                            .orElse(FinanceProducts.builder()
                                    .product(prod)
                                    .finCoNo(base.getFinCoNo())
                                    .productType(type)
                                    .build());

                    // 가입 조건 및 비고
                    String joinCond = String.join("\n",
                            safeLine("가입 방법: ", base.getJoinWay()),
                            safeLine("가입 대상: ", base.getJoinMember()),
                            safeLine("비고: ", base.getEtcNote())
                    ).trim();
                    fp.setJoinCondition(joinCond.isBlank() ? null : joinCond);

                    // 대표 금리 추출
                    BigDecimal rate = extractRepresentativeRate(res, base.getFinPrdtCd(), base.getFinCoNo());
                    fp.setInterestRate(rate);

                    financeProductsRepository.save(fp);
                    saved++;
                }
            }
        }
        log.info("[INGEST] saved count type={} -> {}", type, saved);
        return saved;
    }

    /* -------------------- 대출 상품 / 옵션 동기화 -------------------- */

    /** 모든 대출유형(주택/전세/신용) 일괄 동기화 */
    @Transactional
    public int syncLoans(int maxPages) {
        int total = 0;
        total += syncLoanProductType(FinanceProductType.MORTGAGE_LOAN, maxPages);
        total += syncLoanProductType(FinanceProductType.RENT_HOUSE_LOAN, maxPages);
        total += syncLoanProductType(FinanceProductType.CREDIT_LOAN, maxPages);
        log.info("[INGEST] saved count loans(total) -> {}", total);
        return total;
    }

    /** 대출유형별 분기처리 */
    @Transactional
    public int syncLoanProductType(FinanceProductType type, int maxPages) {
        return switch (type) {
            case MORTGAGE_LOAN -> ingestMortgageLoans(maxPages);
            case RENT_HOUSE_LOAN -> ingestRentLoans(maxPages);
            case CREDIT_LOAN -> ingestCreditLoans(maxPages);
            default -> throw new IllegalArgumentException("Unsupported loan type: " + type);
        };
    }

    /** 주택담보대출(MORTGAGE_LOAN) 동기화 */
    private int ingestMortgageLoans(int maxPages) {
        int saved = 0;
        for (String grp : GROUPS) {
            for (int page = 1; page <= maxPages; page++) {
                FinlifeMortgageLoanResponse res = client.getMortgageLoanProducts(grp, page);
                var bases = (res.getResult() == null) ? null : res.getResult().getBaseList();
                var options = (res.getResult() == null) ? null : res.getResult().getOptionList();
                if (bases == null || bases.isEmpty()) break;

                for (var base : bases) {
                    ensureCompany(base.getFinCoNo(), base.getKorCoNm(), null, null);

                    Products prod = upsertProduct(base.getFinPrdtNm(), base.getKorCoNm(), base.getFinCoNo());
                    FinanceProducts fp = upsertFinanceProduct(prod, base.getFinCoNo(), FinanceProductType.MORTGAGE_LOAN);

                    // 대표 금리
                    BigDecimal rate = extractLendRate(options, base.getFinPrdtCd(), base.getFinCoNo());
                    fp.setInterestRate(rate);
                    financeProductsRepository.save(fp);

                    // 옵션 저장
                    if (options != null) {
                        loanOptionRepository.deleteByFinanceProductId(fp.getId());
                        for (var opt : options) {
                            if (!opt.getFinPrdtCd().equals(base.getFinPrdtCd())) continue;
                            FinanceLoanOption flo = FinanceLoanOption.builder()
                                    .financeProduct(fp)
                                    .lendRateMin(opt.getLendRateMin())
                                    .lendRateMax(opt.getLendRateMax())
                                    .lendRateAvg(opt.getLendRateAvg())
                                    .rpayTypeName(opt.getRpayTypeNm())
                                    .lendTypeName(opt.getLendRateTypeNm())
                                    .mrtgTypeName(opt.getMrtgTypeNm())
                                    .build();
                            loanOptionRepository.save(flo);
                        }
                    }
                    saved++;
                }
            }
        }
        log.info("[INGEST][MORTGAGE] saved count -> {}", saved);
        return saved;
    }

    /** 전세자금대출(RENT_HOUSE_LOAN) 동기화 */
    private int ingestRentLoans(int maxPages) {
        int saved = 0;
        for (String grp : GROUPS) {
            for (int page = 1; page <= maxPages; page++) {
                FinlifeRentLoanResponse res = client.getRentHouseLoanProducts(grp, page);
                var bases = (res.getResult() == null) ? null : res.getResult().getBaseList();
                var options = (res.getResult() == null) ? null : res.getResult().getOptionList();
                if (bases == null || bases.isEmpty()) break;

                for (var base : bases) {
                    ensureCompany(base.getFinCoNo(), base.getKorCoNm(), null, null);

                    Products prod = upsertProduct(base.getFinPrdtNm(), base.getKorCoNm(), base.getFinCoNo());
                    FinanceProducts fp = upsertFinanceProduct(prod, base.getFinCoNo(), FinanceProductType.RENT_HOUSE_LOAN);

                    BigDecimal rate = extractLendRate(options, base.getFinPrdtCd(), base.getFinCoNo());
                    fp.setInterestRate(rate);
                    financeProductsRepository.save(fp);

                    if (options != null) {
                        loanOptionRepository.deleteByFinanceProductId(fp.getId());
                        for (var opt : options) {
                            if (!opt.getFinPrdtCd().equals(base.getFinPrdtCd())) continue;
                            FinanceLoanOption flo = FinanceLoanOption.builder()
                                    .financeProduct(fp)
                                    .lendRateMin(opt.getLendRateMin())
                                    .lendRateMax(opt.getLendRateMax())
                                    .lendRateAvg(opt.getLendRateAvg())
                                    .rpayTypeName(opt.getRpayTypeNm())
                                    .lendTypeName(opt.getLendRateTypeNm())
                                    .build();
                            loanOptionRepository.save(flo);
                        }
                    }
                    saved++;
                }
            }
        }
        log.info("[INGEST][RENT] saved count -> {}", saved);
        return saved;
    }

    /** 개인신용대출(CREDIT_LOAN) 동기화 */
    private int ingestCreditLoans(int maxPages) {
        int saved = 0;
        for (String grp : GROUPS) {
            for (int page = 1; page <= maxPages; page++) {
                FinlifeCreditLoanResponse res = client.getCreditLoanProducts(grp, page);
                var bases = (res.getResult() == null) ? null : res.getResult().getBaseList();
                var options = (res.getResult() == null) ? null : res.getResult().getOptionList();
                if (bases == null || bases.isEmpty()) break;

                for (var base : bases) {
                    ensureCompany(base.getFinCoNo(), base.getKorCoNm(), null, null);

                    Products prod = upsertProduct(base.getFinPrdtNm(), base.getKorCoNm(), base.getFinCoNo());
                    FinanceProducts fp = upsertFinanceProduct(prod, base.getFinCoNo(), FinanceProductType.CREDIT_LOAN);

                    // 대표 금리: crdt_grad_avg 중 최대값
                    BigDecimal avgRate = (options == null) ? null :
                            options.stream()
                                    .filter(o -> base.getFinPrdtCd().equals(o.getFinPrdtCd())
                                            && base.getFinCoNo().equals(o.getFinCoNo()))
                                    .map(FinlifeCreditLoanResponse.Option::getCrdtGradAvg)
                                    .filter(Objects::nonNull)
                                    .max(BigDecimal::compareTo)
                                    .orElse(null);
                    fp.setInterestRate(avgRate);
                    financeProductsRepository.save(fp);

                    // 옵션(creditLoanOption) 저장
                    if (options != null) {
                        // 기존 옵션 제거 후 새로 삽입 (upsert 역할)
                        loanOptionRepository.deleteByFinanceProductId(fp.getId());

                        for (var opt : options) {
                            if (!opt.getFinPrdtCd().equals(base.getFinPrdtCd())) continue;

                            FinanceLoanOption flo = FinanceLoanOption.builder()
                                    .financeProduct(fp)
                                    .lendRateMin(opt.getLendRateMin())
                                    .lendRateMax(opt.getLendRateMax())
                                    .lendRateAvg(opt.getLendRateAvg())
                                    .rpayTypeName(opt.getRpayTypeNm())
                                    .lendTypeName(opt.getLendRateTypeNm())
                                    .mrtgTypeName(null) // 신용대출은 담보유형 없음
                                    // 신용등급별 금리 정보
                                    .crdtLendRateType(opt.getCrdtLendRateType())
                                    .crdtLendRateTypeNm(opt.getCrdtLendRateTypeNm())
                                    .crdtGrad1(opt.getCrdtGrad1())
                                    .crdtGrad4(opt.getCrdtGrad4())
                                    .crdtGrad5(opt.getCrdtGrad5())
                                    .crdtGrad6(opt.getCrdtGrad6())
                                    .crdtGrad10(opt.getCrdtGrad10())
                                    .crdtGrad11(opt.getCrdtGrad11())
                                    .crdtGrad12(opt.getCrdtGrad12())
                                    .crdtGrad13(opt.getCrdtGrad13())
                                    .crdtGradAvg(opt.getCrdtGradAvg())
                                    .build();

                            loanOptionRepository.save(flo);
                        }
                    }

                    saved++;
                }
            }
        }
        log.info("[INGEST][CREDIT] saved count -> {}", saved);
        return saved;
    }

    /* ==================== DB 업서트 헬퍼 ==================== */

    /** Products 엔티티 존재 확인 및 생성 */
    private Products upsertProduct(String name, String provider, String finCoNo) {
        return productsRepository.findByTypeAndNameAndProvider(ProductType.FINANCE, name, provider)
                .orElseGet(() -> productsRepository.save(
                        Products.builder()
                                .type(ProductType.FINANCE)
                                .name(name)
                                .provider(provider)
                                .build()
                ));
    }

    /** FinanceProducts 엔티티 존재 확인 및 생성 */
    private FinanceProducts upsertFinanceProduct(Products prod, String finCoNo, FinanceProductType type) {
        return financeProductsRepository.findByProductAndFinCoNo(prod, finCoNo)
                .orElseGet(() -> financeProductsRepository.save(
                        FinanceProducts.builder()
                                .product(prod)
                                .finCoNo(finCoNo)
                                .productType(type)
                                .build()
                ));
    }

    /* ==================== 금리 추출 로직 ==================== */

    /** 옵션 리스트에서 대표 평균 금리 추출 */
    private BigDecimal extractLendRate(List<?> options, String prdtCd, String finCoNo) {
        if (options == null) return null;
        return options.stream()
                .filter(o -> {
                    try {
                        var cd = (String) o.getClass().getMethod("getFinPrdtCd").invoke(o);
                        var co = (String) o.getClass().getMethod("getFinCoNo").invoke(o);
                        return prdtCd.equals(cd) && finCoNo.equals(co);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(o -> {
                    try {
                        var avg = (BigDecimal) o.getClass().getMethod("getLendRateAvg").invoke(o);
                        var max = (BigDecimal) o.getClass().getMethod("getLendRateMax").invoke(o);
                        var min = (BigDecimal) o.getClass().getMethod("getLendRateMin").invoke(o);
                        if (avg != null) return avg;
                        if (max != null) return max;
                        return min;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);
    }

    /** 예금/적금 대표 금리 추출 */
    private static BigDecimal extractRepresentativeRate(FinlifeProductResponse res, String prdtCd, String finCoNo) {
        var options = (res.getResult() == null) ? null : res.getResult().getOptionList();
        if (options == null) return null;
        return options.stream()
                .filter(o -> prdtCd.equals(o.getFinPrdtCd()) && finCoNo.equals(o.getFinCoNo()))
                .map(o -> o.getInterestRateMax() != null ? o.getInterestRateMax() : o.getInterestRate())
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);
    }

    /** 대출 대표 금리 추출 */
    private static BigDecimal extractRepresentativeLendRate(FinlifeLoanProductResponse res, String prdtCd, String finCoNo) {
        var options = (res.getResult() == null) ? null : res.getResult().getOptionList();
        if (options == null) return null;
        return options.stream()
                .filter(o -> prdtCd.equals(o.getFinPrdtCd()) && finCoNo.equals(o.getFinCoNo()))
                .map(o -> {
                    BigDecimal avg = o.getLendRateAvg();
                    BigDecimal max = o.getLendRateMax();
                    BigDecimal min = o.getLendRateMin();
                    if (avg != null) return avg;
                    if (max != null) return max;
                    return min;
                })
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);
    }

    /* -------------------- 기타 헬퍼 -------------------- */

    /** Null-safe 문자열 합치기 (라인 구성용) */
    private static String safeLine(String label, String val) {
        return (val == null || val.isBlank()) ? "" : (label + val);
    }
}
