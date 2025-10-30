package com.example.capstonedesign.domain.finance.financeproducts.controller;

import com.example.capstonedesign.domain.finance.financeproducts.entity.LoanProductType;
import com.example.capstonedesign.domain.finance.financeproducts.service.FinanceLoanQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 금융 대출상품 조회 컨트롤러
 * - 대출 유형(주택담보 / 전세자금 / 개인신용)에 따라 다른 DTO를 반환
 * - Service 레이어의 대출 조회 기능(FinanceLoanQueryService)과 연결
 */
@Tag(name = "Finance Loan Options", description = "대출 금리 및 상품 정보 조회 API")
@RestController
@RequestMapping("/api/finance/loans/options")
@RequiredArgsConstructor
public class FinanceLoansController {

    private final FinanceLoanQueryService loanQueryService;

    /**
     * 대출 종류별 옵션/금리 조회
     *
     * @param loanType 대출 유형 (MORTGAGE_LOAN / RENT_HOUSE_LOAN / CREDIT_LOAN)
     * @return 대출 상품 및 금리 옵션 DTO 리스트
     * <p>
     * 예시:
     * GET /api/finance/loans/options/type/MORTGAGE_LOAN
     */
    @Operation(summary = "대출 종류별 옵션 조회", description = """
        대출 종류(주택담보대출, 전세자금대출, 개인신용대출)에 해당하는\s
        모든 상품의 금리 및 옵션 정보를 반환합니다.
       \s""")
    @GetMapping("/type/{loanType}")
    public ResponseEntity<?> getLoanOptionsByType(@PathVariable LoanProductType loanType) {
        return switch (loanType) {
            case MORTGAGE_LOAN -> ResponseEntity.ok(loanQueryService.getMortgageLoans());
            case RENT_HOUSE_LOAN -> ResponseEntity.ok(loanQueryService.getRentLoans());
            case CREDIT_LOAN -> ResponseEntity.ok(loanQueryService.getCreditLoans());
        };
    }

    /**
     * 사용자 맞춤 대출 추천 API
     * -------------------------------------------------
     * - 사용자 나이, 소득 구간(중위소득 100~300%), 평균 금리를 기반으로 추천
     * - 낮은 금리 + 청년층(20~35세) + 저소득층(중위소득 150% 이하)일수록 상위 추천
     * - 소득 구간별로 추천 상품 유형이 달라짐:
     *     · 100~150% 이하 → 전세자금대출 / 주택담보대출 중심
     *     · 200% 이하 → 주담대 + 일부 신용대출
     *     · 300% 이하 → 신용대출 중심
     *
     * @param userId 사용자 ID
     * @return 상위 10개의 맞춤 대출 추천 리스트
     * <p>
     * 예시 요청:
     * GET /api/finance/loans/recommend/3
     */
    @Operation(summary = "사용자 맞춤 대출 추천", description = """
    사용자 나이, 소득 구간(중위소득 100~300%), 금리평균을 기준으로
    맞춤형 대출 상품을 추천합니다.

    추천 로직 요약:
    - 청년층(20~35세)과 저소득층(150% 이하)은 전세·주담대 중심 추천
    - 중·고소득층은 금리가 낮은 신용대출 중심 추천
    - 평균 금리가 낮을수록, 점수가 낮을수록 상위 노출
    - 상위 10개의 추천 상품만 반환됩니다.
    """)
    @GetMapping("/recommend/{userId}")
    public ResponseEntity<?> recommendLoansForUser(
            @Parameter(description = "사용자 ID") @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(loanQueryService.recommendLoansForUser(userId));
    }

}