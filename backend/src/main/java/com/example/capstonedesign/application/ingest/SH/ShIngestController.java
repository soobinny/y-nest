package com.example.capstonedesign.application.ingest.SH;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ShIngestController
 * -------------------------------------------------
 * - SH 공고 수집 작업을 수동으로 트리거하기 위한 임시 엔드포인트
 * - POST /admin/sh/sync 호출 시 {@link ShIngestService#crawlAll()} 실행
 */
@RestController
@RequestMapping("/admin/sh")
@RequiredArgsConstructor
public class ShIngestController {

    private final ShIngestService shIngestService;

    @PostMapping("/sync")
    public ResponseEntity<Void> sync() {
        shIngestService.crawlAll();
        return ResponseEntity.ok().build();
    }
}
