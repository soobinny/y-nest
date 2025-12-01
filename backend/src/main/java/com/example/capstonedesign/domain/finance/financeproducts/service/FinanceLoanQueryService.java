package com.example.capstonedesign.domain.finance.financeproducts.service;

import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinanceLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.MortgageLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.RentLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceLoanOption;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceLoanOptionRepository;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceProductsRepository;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FinanceLoanQueryService
 * -------------------------------------------------
 * 대출 상품 조회 및 사용자 맞춤 추천 서비스
 * - 주택담보대출 / 전세자금대출 / 신용대출 조회
 * - 금리·상환옵션 및 사용자 조건 기반 추천 제공
 */
@Service
@RequiredArgsConstructor
public class FinanceLoanQueryService {

    private final UsersRepository usersRepository;
    private final FinanceProductsRepository financeProductsRepository;
    private final FinanceLoanOptionRepository loanOptionRepository;

    /**
     * 주택담보대출 목록 조회
     * - MORTGAGE_LOAN 타입 상품 + 금리/상환/담보 옵션
     */
    public List<MortgageLoanResponse> getMortgageLoans() {
        List<FinanceProducts> products = financeProductsRepository.findByProductType(FinanceProductType.MORTGAGE_LOAN);

        return products.stream().flatMap(fp ->
                loanOptionRepository.findByFinanceProduct(fp).stream().map(opt ->
                        MortgageLoanResponse.builder()
                                .productId(fp.getProduct().getId())
                                .productName(fp.getProduct().getName())
                                .companyName(fp.getProduct().getProvider())
                                .productType(fp.getProductType())
                                .lendRateMin(opt.getLendRateMin())
                                .lendRateMax(opt.getLendRateMax())
                                .lendRateAvg(opt.getLendRateAvg())
                                .lendTypeName(opt.getLendTypeName())
                                .rpayTypeName(opt.getRpayTypeName())
                                .mrtgTypeName(opt.getMrtgTypeName())
                                .build()
                )
        ).collect(Collectors.toList());
    }

    /**
     * 전세자금대출 목록 조회
     * - RENT_HOUSE_LOAN 타입 상품 + 금리/상환 옵션
     */
    public List<RentLoanResponse> getRentLoans() {
        List<FinanceProducts> products = financeProductsRepository.findByProductType(FinanceProductType.RENT_HOUSE_LOAN);

        return products.stream().flatMap(fp ->
                loanOptionRepository.findByFinanceProduct(fp).stream().map(opt ->
                        RentLoanResponse.builder()
                                .productId(fp.getProduct().getId())
                                .productName(fp.getProduct().getName())
                                .companyName(fp.getProduct().getProvider())
                                .productType(fp.getProductType())
                                .lendRateMin(opt.getLendRateMin())
                                .lendRateMax(opt.getLendRateMax())
                                .lendRateAvg(opt.getLendRateAvg())
                                .lendTypeName(opt.getLendTypeName())
                                .rpayTypeName(opt.getRpayTypeName())
                                .build()
                )
        ).collect(Collectors.toList());
    }

    /**
     * 개인신용대출 목록 조회
     * - CREDIT_LOAN 타입 상품
     * - 등급별 금리(crdtGrad1~13) 포함
     */
    public List<FinanceLoanResponse> getCreditLoans() {
        List<FinanceProducts> loans = financeProductsRepository.findByProductType(FinanceProductType.CREDIT_LOAN);

        return loans.stream().map(product -> {
                    FinanceLoanOption option = loanOptionRepository.findTopByFinanceProduct(product);
                    if (option == null) return null;

                    return FinanceLoanResponse.builder()
                            .productId(product.getProduct().getId())
                            .productName(product.getProduct().getName())
                            .companyName(product.getProduct().getProvider())
                            .productType(product.getProductType())
                            .crdtLendRateType(option.getCrdtLendRateType())
                            .crdtLendRateTypeNm(option.getCrdtLendRateTypeNm())
                            .crdtGrad1(option.getCrdtGrad1())
                            .crdtGrad4(option.getCrdtGrad4())
                            .crdtGrad5(option.getCrdtGrad5())
                            .crdtGrad6(option.getCrdtGrad6())
                            .crdtGrad10(option.getCrdtGrad10())
                            .crdtGrad11(option.getCrdtGrad11())
                            .crdtGrad12(option.getCrdtGrad12())
                            .crdtGrad13(option.getCrdtGrad13())
                            .crdtGradAvg(option.getCrdtGradAvg())
                            .build();
                })
                .filter(l -> l != null)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 맞춤 대출 추천
     * -------------------------------------------------
     * - 나이, 소득 구간, 평균 금리, 대출 유형 기반으로 종합 추천
     * - 저소득층은 전세/주담대 중심, 고소득층은 신용대출 중심
     * - 점수(score)가 낮을수록 추천 순위가 높음
     */
    public List<FinanceLoanResponse> recommendLoansForUser(Integer userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int age = user.getAge();
        String incomeBand = user.getIncome_band().replace(" ", "");

        // 소득 구간별 대출유형 필터링
        List<FinanceProductType> targetTypes = switch (incomeBand) {
            case "중위소득100%이하", "중위소득150%이하" -> List.of(
                    FinanceProductType.RENT_HOUSE_LOAN,
                    FinanceProductType.MORTGAGE_LOAN
            );
            case "중위소득200%이하" -> List.of(
                    FinanceProductType.MORTGAGE_LOAN,
                    FinanceProductType.CREDIT_LOAN
            );
            case "중위소득300%이하" -> List.of(
                    FinanceProductType.CREDIT_LOAN
            );
            default -> List.of(
                    FinanceProductType.MORTGAGE_LOAN,
                    FinanceProductType.RENT_HOUSE_LOAN,
                    FinanceProductType.CREDIT_LOAN
            );
        };

        List<FinanceProducts> allLoans = financeProductsRepository.findByProductTypeIn(targetTypes);

        // 상품별 평균 금리와 조건을 종합하여 추천 계산
        return allLoans.stream()
                .flatMap(product -> loanOptionRepository.findByFinanceProduct(product).stream()
                        .filter(opt -> opt.getLendRateAvg() != null || opt.getCrdtGradAvg() != null)
                        .map(opt -> {
                            BigDecimal avgRate = opt.getLendRateAvg() != null
                                    ? opt.getLendRateAvg()
                                    : opt.getCrdtGradAvg();

                            double score = calculateRecommendationScore(age, incomeBand, product.getProductType(), avgRate);
                            String reason = getRecommendationReason(age, incomeBand, product.getProductType(), avgRate);

                            return FinanceLoanResponse.builder()
                                    .productId(product.getProduct().getId())
                                    .productName(product.getProduct().getName())
                                    .companyName(product.getProduct().getProvider())
                                    .productType(product.getProductType())
                                    .lendRateAvg(opt.getLendRateAvg())
                                    .crdtGradAvg(opt.getCrdtGradAvg())
                                    .score(score)
                                    .reason(reason)
                                    .build();
                        })
                )
                .distinct()
                .sorted(Comparator.comparingDouble(FinanceLoanResponse::getScore))
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * 추천 점수 계산 (낮을수록 우선순위 ↑)
     * -------------------------------------------------
     * - 금리 × 연령 × 소득 × 대출유형 가중치 반영
     */
    private double calculateRecommendationScore(int age, String incomeBand, FinanceProductType type, BigDecimal avgRate) {
        double rate = avgRate != null ? avgRate.doubleValue() : 10.0;

        // 연령 가중치
        double ageFactor;
        if (age >= 20 && age <= 35) ageFactor = 0.8;
        else if (age >= 36 && age <= 50) ageFactor = 1.0;
        else ageFactor = 1.2;

        // 소득 가중치 (소득 낮을수록 우대)
        double incomeFactor = switch (incomeBand) {
            case "중위소득100%이하" -> 0.7;
            case "중위소득150%이하" -> 0.85;
            case "중위소득200%이하" -> 1.0;
            case "중위소득300%이하" -> 1.15;
            default -> 1.0;
        };

        // 대출유형 가중치 (소득·연령에 따른 우대)
        double typeFactor = 1.0;
        switch (type) {
            case RENT_HOUSE_LOAN -> {
                if (incomeBand.contains("100%") || incomeBand.contains("150%") || age <= 35)
                    typeFactor = 0.8; // 청년·저소득층 전세자금대출 우대
            }
            case MORTGAGE_LOAN -> {
                if (age >= 40 && (incomeBand.contains("150%") || incomeBand.contains("200%")))
                    typeFactor = 0.9; // 중장년·중위소득층 주담대 우대
            }
            case CREDIT_LOAN -> {
                if (incomeBand.contains("300%")) typeFactor = 0.8; // 고소득층 신용대출 우대
                else typeFactor = 1.2; // 저소득층엔 비추천
            }
        }

        return (rate * 0.6) * ageFactor * incomeFactor * typeFactor;
    }

    /**
     * 추천 사유 생성
     * -------------------------------------------------
     * - 금리, 연령, 소득 구간, 대출 유형에 따른 설명 문자열 반환
     */
    private String getRecommendationReason(int age, String incomeBand, FinanceProductType type, BigDecimal avgRate) {
        StringBuilder reason = new StringBuilder();

        if (avgRate != null)
            reason.append("평균 금리 ").append(avgRate).append("%, ");

        // 연령 설명
        if (age >= 20 && age <= 35) reason.append("청년층 우대, ");
        else if (age >= 40) reason.append("중장년층 적합, ");

        // 소득 구간 설명
        switch (incomeBand) {
            case "중위소득100%이하" -> reason.append("저소득층 대상 상품, ");
            case "중위소득150%이하" -> reason.append("중저소득층 혜택 상품, ");
            case "중위소득200%이하" -> reason.append("보통 소득층 적합 상품, ");
            case "중위소득300%이하" -> reason.append("고소득층 신용대출 우대, ");
        }

        // 대출유형 설명
        switch (type) {
            case RENT_HOUSE_LOAN -> reason.append("전세자금대출 중심");
            case MORTGAGE_LOAN -> reason.append("주택담보대출 중심");
            case CREDIT_LOAN -> reason.append("신용대출 중심");
        }

        return reason.toString().replaceAll(", $", ""); // 마지막 쉼표 제거
    }
}
