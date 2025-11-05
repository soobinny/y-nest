package com.example.capstonedesign.domain.finance.financeproducts.controller;

import com.example.capstonedesign.domain.finance.FinanceProductSpecs;
import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinanceProductsResponse;
import com.example.capstonedesign.domain.finance.financeproducts.entity.DSProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceProductsRepository;
import com.example.capstonedesign.domain.finance.financeproducts.service.FinanceProductRecommendationService;
import com.example.capstonedesign.domain.products.entity.Products;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * FinanceProductQueryController
 * -------------------------------------------------
 * - 금융 상품 목록 조회 + 사용자 맞춤 추천 API
 * - Specification 기반 동적 필터 및 정렬
 * - 예금·적금·대출 상품 검색 및 추천 제공
 */
@Tag(name = "Finance Products", description = "금융 상품 조회 및 추천 API")
@RestController
@RequestMapping("/api/finance/products")
@RequiredArgsConstructor
public class FinanceProductQueryController {

    /** 금융 상품 JPA 레포지토리 */
    private final FinanceProductsRepository repo;

    /** 사용자 맞춤 추천 서비스 */
    private final FinanceProductRecommendationService recommendService;

    /**
     * 안전한 정렬 필드 화이트리스트
     */
    private static final Map<String, String> SORT_MAP;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", "id");
        m.put("interestRate", "interestRate");
        m.put("interest_rate", "interestRate");
        m.put("minDeposit", "minDeposit");
        m.put("min_deposit", "minDeposit");
        m.put("productName", "product.name");
        m.put("product_name", "product.name");
        m.put("productType", "productType");
        m.put("product_type", "productType");
        m.put("finCoNo", "finCoNo");
        m.put("fin_co_no", "finCoNo");
        SORT_MAP = Collections.unmodifiableMap(m);
    }

    /**
     * Pageable의 정렬 키를 화이트리스트로 필터링
     */
    private Pageable sanitizePageable(Pageable pageable) {
        List<Sort.Order> safeOrders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String key = order.getProperty();
            if (key == null) continue;
            String k = key.trim();
            if ("string".equalsIgnoreCase(k) || k.startsWith("[") || k.endsWith("]")) continue;

            String mapped = SORT_MAP.get(k);
            if (mapped != null && !mapped.isBlank()) {
                safeOrders.add(new Sort.Order(order.getDirection(), mapped));
            }
        }

        Sort safeSort = safeOrders.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "id")
                : Sort.by(safeOrders);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), safeSort);
    }

    /**
     * 금융 상품 목록 조회
     * -------------------------------------------------
     * 지원 기능:
     * - productType: 예금/적금/대출 필터
     * - finCoNo: 금융 회사 코드
     * - keyword: 상품명·회사명 like 검색
     * - minRate / maxRate: 금리 구간
     * - 페이지네이션 + 안전한 정렬
     */
    @Operation(
            summary = "금융 상품 목록 조회",
            description = """
                    예금·적금·대출/회사 코드/키워드(상품명·회사명)/금리 범위 필터 + 페이지네이션/정렬
                    정렬 예시: ?sort=id,desc 또는 ?sort=interestRate,asc
                    (Swagger 기본값 'string'은 무시되며, 허용되지 않은 키는 자동 무시됩니다.)
                    """
    )
    @GetMapping
    public Page<FinanceProductsResponse> list(
            @Parameter(description = "상품 종류: DEPOSIT(정기예금), SAVING(적금), MORTGAGE_LOAN(주택담보대출), RENT_HOUSE_LOAN(전세자금대출), CREDIT_LOAN(개인신용대출)")
            @RequestParam(required = false) FinanceProductType productType,
            @Parameter(description = "금융 회사 코드(fin_co_no)")
            @RequestParam(required = false) String finCoNo,
            @Parameter(description = "키워드(상품명/회사명 like 검색)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "금융기관(provider) 리스트")
            @RequestParam(required = false) List<String> providers,
            @Parameter(description = "제외할 금융기관 리스트 (\"그 외\" 선택 시 사용)")
            @RequestParam(required = false) List<String> excludeProviders,
            @Parameter(description = "최소 금리(예: 3.0)")
            @RequestParam(required = false) BigDecimal minRate,
            @Parameter(description = "최대 금리(예: 6.0)")
            @RequestParam(required = false) BigDecimal maxRate,
            @ParameterObject
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Specification<FinanceProducts> spec = Specification.allOf(
                FinanceProductSpecs.productType(productType),
                FinanceProductSpecs.finCoNo(finCoNo),
                FinanceProductSpecs.keyword(keyword),
                FinanceProductSpecs.providers(providers, excludeProviders),
                FinanceProductSpecs.minRate(minRate),
                FinanceProductSpecs.maxRate(maxRate)
        );

        Pageable safePageable = sanitizePageable(pageable);
        Page<FinanceProducts> page = repo.findAll(spec, safePageable);
        return page.map(this::toDto);
    }

    /**
     * 사용자 맞춤 추천 API
     * -------------------------------------------------
     * 사용자 정보(나이·소득·지역)에 기반한 예금/적금 추천
     * 예시: /api/finance/products/recommend/{userId}?type=SAVING
     */
    @Operation(summary = "사용자 맞춤 예금/적금 추천", description = """
    사용자의 나이, 소득 구간(중위소득 100~300%), 금리, 예치금 조건을 기준으로 맞춤형 예금·적금 상품을 추천합니다.

    추천 로직 요약:
    - 청년층(20~35세) 및 저소득층(150% 이하): 소액 예치 가능하고 금리가 높은 상품 우대
    - 중위·고소득층(200~300%): 예치금 규모가 크고 안정적인 상품 중심 추천
    - 금리가 높을수록, 예치금이 낮을수록, 점수가 낮을수록 상위 노출
    - 내부적으로 산출된 종합 점수에 따라 상위 10개의 추천 상품 반환
    """)
    @GetMapping("/recommend/{userId}")
    public List<FinanceProductsResponse> recommendForUser(
            @Parameter(description = "사용자 ID") @PathVariable Integer userId,
            @Parameter(description = "조회할 예금·적금 상품 선택(DEPOSIT: 예금, SAVING: 적굼)")
            @RequestParam(defaultValue = "DEPOSIT") DSProductType type
    ) {
        // DSProductType → FinanceProductType 매핑 필요
        FinanceProductType productType = switch (type) {
            case DEPOSIT -> FinanceProductType.DEPOSIT;
            case SAVING -> FinanceProductType.SAVING;
        };

        return recommendService.recommendDepositOrSaving(userId, productType);
    }

    /**
     * 엔티티 → 응답 DTO 변환
     */
    private FinanceProductsResponse toDto(FinanceProducts fp) {
        Products p = fp.getProduct();
        return FinanceProductsResponse.builder()
                .id(fp.getId())
                .productId(p != null ? p.getId() : null)
                .productName(p != null ? p.getName() : null)
                .provider(p != null ? p.getProvider() : null)
                .detailUrl(p != null ? p.getDetailUrl() : null)
                .finCoNo(fp.getFinCoNo())
                .productType(fp.getProductType())
                .interestRate(fp.getInterestRate())
                .minDeposit(fp.getMinDeposit())
                .joinCondition(fp.getJoinCondition())
                .build();
    }
}
