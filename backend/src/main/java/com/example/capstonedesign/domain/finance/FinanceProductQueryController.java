package com.example.capstonedesign.domain.finance;

import com.example.capstonedesign.domain.finance.financeproducts.dto.response.FinanceProductsResponse;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProductType;
import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceProducts;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceProductsRepository;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;

/**
 * 금융 상품 조회 전용 컨트롤러
 * - Specification 조합으로 동적 필터 (상품 유형/회사 코드/키워드/금리 범위)
 * - 안전한 정렬 키 화이트리스트(SORT_MAP)로 정렬 오용/오류 방지
 * - Page/Sort sanitize로 Swagger 기본값("string") 등 노이즈 제거
 */
@Tag(name = "Finance Products", description = "금융 상품 조회 API")
@RestController
@RequestMapping("/api/finance/products")
@RequiredArgsConstructor
public class FinanceProductQueryController {

    /** 금융 상품 JPA 레포지토리 */
    private final FinanceProductsRepository repo;

    /**
     * 외부에서 들어온 sort 키를 엔티티 실제 필드명으로 안전하게 매핑
     * - Swagger 기본 예시 값("string", ["string"])은 무시
     * - 허용되지 않은 키는 무시
     * - 유효한 값이 하나도 없으면 id DESC로 대체
     */
    private static final Map<String, String> SORT_MAP;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        // 공통 PK
        m.put("id", "id");

        // 금리
        m.put("interestRate", "interest_rate");
        m.put("interest_rate", "interest_rate");

        // 최소 예치액
        m.put("minDeposit", "min_deposit");
        m.put("min_deposit", "min_deposit");

        // 상품 유형
        m.put("productType", "productType");
        m.put("product_type", "product_type");

        // 회사 코드
        m.put("finCoNo", "finCoNo");
        m.put("fin_co_no", "fin_co_no");

        SORT_MAP = Collections.unmodifiableMap(m);
    }

    /**
     * 전달된 Pageable의 정렬 정보를 화이트리스트로 필터링하여 안전한 Pageable로 반환
     * - 허용되지 않은 정렬 키는 버리고, 하나도 남지 않으면 id DESC를 기본값으로 사용
     */
    private Pageable sanitizePageable(Pageable pageable) {
        List<Sort.Order> safeOrders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String key = order.getProperty();
            if (key == null) continue;

            // Swagger 기본 예시/노이즈 값 제거
            String k = key.trim();
            if ("string".equalsIgnoreCase(k) || k.startsWith("[") || k.endsWith("]")) {
                continue;
            }

            // 화이트리스트 매핑
            String mapped = SORT_MAP.get(k);
            if (mapped == null || mapped.isBlank()) {
                // 허용되지 않은 정렬 키는 무시
                continue;
            }
            // 정렬 방향은 그대로 유지, 필드명만 안전한 것으로 대체
            safeOrders.add(new Sort.Order(order.getDirection(), mapped));
        }

        // 하나도 남지 않으면 id DESC 기본값
        Sort safeSort = safeOrders.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "id")
                : Sort.by(safeOrders);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), safeSort);
    }

    /**
     * 금융 상품 목록 조회
     * <p>
     * 지원 기능:
     * - productType: 예금/적금/대출 상품 필터
     * - finCoNo:     금융 회사 코드 필터
     * - keyword: products.name / products.provider LIKE 검색
     * - minRate / maxRate: 금리 구간 필터
     * - 페이지네이션/정렬:    안전한 키만 허용
     */
    @Operation(
            summary = "금융 상품 목록 조회",
            description = """
                    예금·적금·대출/회사 코드/키워드(상품명·회사명)/금리 범위 필터 + 페이지네이션/정렬
                    정렬 예시: ?sort=id,desc 또는 ?sort=interestRate,asc
                    (Swagger 기본값 'string'은 무시되며, 허용되지 않은 키는 자동 무시됩니다.)"""
    )
    @GetMapping
    public Page<FinanceProductsResponse> list(
            @Parameter(description = "상품 종류: DEPOSIT(정기예금), SAVING(적금), MORTGAGE_LOAN(주택담보대출), RENT_HOUSE_LOAN(전세자금대출), CREDIT_LOAN(개인신용대출)")
            @RequestParam(required = false) FinanceProductType productType,
            @Parameter(description = "금융 회사 코드(fin_co_no)")
            @RequestParam(required = false) String finCoNo,
            @Parameter(description = "키워드(상품명/회사명 like 검색)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "최소 금리(예: 3.0)")
            @RequestParam(required = false) BigDecimal minRate,
            @Parameter(description = "최대 금리(예: 6.0)")
            @RequestParam(required = false) BigDecimal maxRate,
            @ParameterObject
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 1) Specification 조합으로 동적 필터 구성
        Specification<FinanceProducts> spec = Specification.allOf(
                FinanceProductSpecs.productType(productType),
                FinanceProductSpecs.finCoNo(finCoNo),
                FinanceProductSpecs.keyword(keyword),
                FinanceProductSpecs.minRate(minRate),
                FinanceProductSpecs.maxRate(maxRate)
        );

        // 2) 외부 정렬 키를 안전하게 정제
        Pageable safePageable = sanitizePageable(pageable);

        // 3) 조회 및 DTO 매핑
        Page<FinanceProducts> page = repo.findAll(spec, safePageable);
        return page.map(this::toDto);
    }

    /**
     * 엔티티 → 응답 DTO 변환
     * - NPE 방지를 위해 연관 엔티티(products) 접근 시 null 체크 수행
     * - 현재 프로젝트 컨벤션에 맞춰 snake_case 게터(getInterest_rate 등)를 사용
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
                .interestRate(fp.getInterestRate())   // 엔티티가 snake_case 필드/게터를 쓰는 경우
                .minDeposit(fp.getMinDeposit())       // 동일
                .joinCondition(fp.getJoinCondition()) // 동일
                .build();
    }
}