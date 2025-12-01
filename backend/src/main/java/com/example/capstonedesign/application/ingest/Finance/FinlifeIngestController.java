package com.example.capstonedesign.application.ingest.Finance;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MVP 편의용 수동 트리거
 */
@RestController
@RequestMapping("/admin/ingest/finlife")
@RequiredArgsConstructor
public class FinlifeIngestController {

    private final FinlifeIngestService service;

    @Operation(summary = "금융 기관 동기화", description = "finance_companies upsert")
    @PostMapping("/companies")
    public ResponseEntity<String> ingestCompanies(@RequestParam(defaultValue = "5") int maxPages) {
        int count = service.syncCompanies(maxPages);
        return ResponseEntity.ok("companies upserted: " + count);
    }

    @Operation(summary = "예·적금 상품 동기화", description = "products/finance_products upsert")
    @PostMapping("/products")
    public ResponseEntity<String> ingestProducts(@RequestParam(defaultValue = "10") int maxPages) {
        int count = service.syncDepositAndSaving(maxPages);
        return ResponseEntity.ok("finance_products upserted: " + count);
    }

    @Operation(summary = "회사+상품 전체 동기화")
    @PostMapping("/all")
    public ResponseEntity<String> ingestAll(@RequestParam(defaultValue = "5") int companyPages,
                                            @RequestParam(defaultValue = "10") int productPages) {
        int c = service.syncCompanies(companyPages);
        int p = service.syncDepositAndSaving(productPages);
        return ResponseEntity.ok("done - companies: " + c + ", products: " + p);
    }

    @Operation(summary = "대출 상품 동기화", description = "products/finance_products/loan_options upsert")
    @PostMapping("/loans")
    public ResponseEntity<String> ingestLoans(@RequestParam(defaultValue = "10") int maxPages) {
        int count = service.syncLoans(maxPages);
        return ResponseEntity.ok("loan products upserted: " + count);
    }
}
