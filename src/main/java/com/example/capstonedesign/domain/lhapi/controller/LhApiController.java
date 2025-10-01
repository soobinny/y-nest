package com.example.capstonedesign.domain.lhapi.controller;

import com.example.capstonedesign.domain.lhapi.service.LhApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LhApiController {

    private final LhApiService lhApiService;

    @GetMapping("/api/lh/notices")
    public String getNotices(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size) {
        return lhApiService.getLeaseNotices(page, size);
    }
}
