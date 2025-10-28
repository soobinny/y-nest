package com.example.capstonedesign.domain.finance.financeproducts.service;

import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinanceLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.MortgageLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.RentLoanResponse;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceLoanOption;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceLoanOptionRepository;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceProductsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 대출 상품 조회 서비스
 * - 주택담보대출, 전세자금대출, 개인신용대출을 각각 조회
 * - 상품 기본정보(FinanceProducts) + 세부 금리옵션(FinanceLoanOption) 결합
 */
@Service
@RequiredArgsConstructor
public class FinanceLoanQueryService {

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
}
