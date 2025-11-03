package com.example.capstonedesign.application.ingest.LH;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LH 분양·임대 공고 수동 수집용 컨트롤러
 * -------------------------------------------------
 * - Swagger에서 수동으로 공고 수집 실행 가능
 * - LH 공공데이터포털 API(lhLeaseNoticeInfo1) 연동
 */
@Slf4j
@Tag(name = "LH Lease Notice", description = "LH 분양·임대 공고 수집 API")
@RestController
@RequestMapping("/admin/ingest/lh")
@RequiredArgsConstructor
public class LhLeaseNoticeController {

    private final LhLeaseNoticeService lhLeaseNoticeService;

    @Operation(summary = "LH 분양·임대 공고 수동 수집", description = """
        LH 공공데이터포털의 '분양·임대공고문 조회 API'를 이용해
        실시간으로 공고 데이터를 수집합니다.<br><br>
        - 자동 스케줄러 외에 수동으로 테스트할 때 사용됩니다.<br>
        - 중복 공고는 자동으로 필터링되어 저장되지 않습니다.
        """)
    @PostMapping("/lease")
    public ResponseEntity<String> ingestLeaseNotices() {
        log.info("🧩 [수동 실행] LH 분양·임대 공고 수집 시작");
        lhLeaseNoticeService.fetchNotices();
        return ResponseEntity.ok("✅ LH 분양·임대 공고 수집이 완료되었습니다.");
    }
}
