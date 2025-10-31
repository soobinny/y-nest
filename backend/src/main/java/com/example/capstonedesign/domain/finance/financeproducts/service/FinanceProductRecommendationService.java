package com.example.capstonedesign.domain.finance.financeproducts.service;

import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinanceProductsResponse;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
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
 * FinanceProductRecommendationService
 * ---------------------------------------------
 * - 사용자 정보 기반 예금/적금 상품 추천 서비스
 * - 사용자 소득대역, 연령, 금리 조건 등을 종합하여 맞춤형 추천 제공
 */
@Service
@RequiredArgsConstructor
public class FinanceProductRecommendationService {

    private final UsersRepository usersRepository;
    private final FinanceProductsRepository financeProductsRepository;

    /**
     * 사용자 맞춤 예금/적금 추천
     * -------------------------------------------------
     * @param userId 사용자 ID
     * @param type   예금(DEPOSIT) 또는 적금(SAVING)
     */
    public List<FinanceProductsResponse> recommendDepositOrSaving(Integer userId, FinanceProductType type) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int age = user.getAge();
        String incomeBand = user.getIncome_band().replace(" ", "");

        // DSProductType → FinanceProductType 변환
        FinanceProductType productType = switch (type) {
            case DEPOSIT -> FinanceProductType.DEPOSIT;
            case SAVING -> FinanceProductType.SAVING;
            default -> throw new IllegalArgumentException("예금/적금 외 타입은 지원하지 않습니다.");
        };

        // 변환된 타입으로 조회
        List<FinanceProducts> products = financeProductsRepository.findByProductType(productType);

        // 각 상품에 점수 부여 후 정렬
        return products.stream()
                .filter(p -> p.getInterestRate() != null)
                .map(p -> {
                    double score = calculateScore(age, incomeBand, p.getInterestRate(), p.getMinDeposit());
                    String reason = getRecommendationReason(age, incomeBand, p, productType);

                    return FinanceProductsResponse.builder()
                            .id(p.getId())
                            .productName(p.getProduct().getName())
                            .provider(p.getProduct().getProvider())
                            .productType(p.getProductType())
                            .interestRate(p.getInterestRate())
                            .minDeposit(p.getMinDeposit())
                            .joinCondition(p.getJoinCondition())
                            .score(score)
                            .reason(reason)
                            .build();
                })
                .sorted(Comparator.comparingDouble(FinanceProductsResponse::getScore))
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * 추천 점수 계산 (낮을수록 우선)
     * -------------------------------------------------
     * - 금리(높을수록 우대)
     * - 예치금(낮을수록 우대)
     * - 연령, 소득대역 가중치
     */
    private double calculateScore(int age, String incomeBand, BigDecimal interestRate, Integer minDeposit) {
        double rate = interestRate.doubleValue();
        double deposit = minDeposit != null ? minDeposit.doubleValue() : 0.0;

        // 금리: 높을수록 유리 → 역비율
        double rateFactor = 10.0 / rate;

        // 예치금: 낮을수록 유리
        double depositFactor = deposit > 0 ? Math.log(deposit / 100000 + 1) : 1.0;

        // 연령 가중치 (청년층 우대)
        double ageFactor;
        if (age <= 34) ageFactor = 0.8;
        else if (age <= 50) ageFactor = 1.0;
        else ageFactor = 1.2;

        // 소득 가중치 (소득 낮을수록 우대)
        double incomeFactor = switch (incomeBand) {
            case "중위소득100%이하" -> 0.7;
            case "중위소득150%이하" -> 0.85;
            case "중위소득200%이하" -> 1.0;
            case "중위소득300%이하" -> 1.1;
            default -> 1.0;
        };

        return rateFactor * depositFactor * ageFactor * incomeFactor;
    }

    /**
     * 추천 사유 문자열 생성
     */
    private String getRecommendationReason(int age, String incomeBand, FinanceProducts p, FinanceProductType type) {
        StringBuilder sb = new StringBuilder();

        if (p.getInterestRate() != null)
            sb.append("금리 ").append(p.getInterestRate()).append("%, ");
        if (p.getMinDeposit() != null)
            sb.append("최소 예치금 ").append(p.getMinDeposit()).append("원, ");

        if (age <= 34) sb.append("청년층 우대 가능, ");
        else if (age >= 50) sb.append("안정형 투자에 적합, ");

        switch (incomeBand) {
            case "중위소득100%이하" -> sb.append("저소득층 혜택 상품, ");
            case "중위소득150%이하" -> sb.append("중저소득층 추천 상품, ");
            case "중위소득200%이하" -> sb.append("일반 소득층 상품, ");
            case "중위소득300%이하" -> sb.append("고소득층 고금리 상품, ");
        }

        sb.append(type == FinanceProductType.SAVING ? "적금 상품" : "예금 상품");
        return sb.toString().replaceAll(", $", "");
    }
}
