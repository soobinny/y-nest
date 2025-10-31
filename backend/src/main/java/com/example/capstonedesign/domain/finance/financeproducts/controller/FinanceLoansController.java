package com.example.capstonedesign.domain.finance.financeproducts.controller;

import com.example.capstonedesign.domain.finance.financeproducts.entity.LoanProductType;
import com.example.capstonedesign.domain.finance.financeproducts.service.FinanceLoanQueryService;
import io.swagger.v3.oas.annotations.Operation;
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
}