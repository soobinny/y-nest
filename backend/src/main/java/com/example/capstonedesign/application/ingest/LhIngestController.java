package com.example.capstonedesign.application.ingest;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LH 공고 수집 수동 트리거 컨트롤러
 * - Swagger에서 POST /admin/ingest/lh/all 실행 가능
 * - LhHousingIngestService.ingest() 직접 호출
 */
@RestController
@RequestMapping("/admin/ingest/lh")
@RequiredArgsConstructor
public class LhIngestController {

    private final LhHousingIngestService service;

    @Operation(summary = "LH 공고 전체 수집", description = "임대/분양 공고 모두 수집 및 DB 저장")
    @PostMapping("/all")
    public ResponseEntity<String> ingestAll() {
        service.ingest();
        return ResponseEntity.ok("LH 공고 수집 완료");
    }
}