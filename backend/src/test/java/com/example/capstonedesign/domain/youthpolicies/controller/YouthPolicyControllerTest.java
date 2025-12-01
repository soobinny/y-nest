package com.example.capstonedesign.domain.youthpolicies.controller;

import com.example.capstonedesign.application.ingest.Youth.YouthPolicyIngestService;
import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import com.example.capstonedesign.domain.youthpolicies.dto.response.YouthPolicyResponse;
import com.example.capstonedesign.domain.youthpolicies.service.YouthPolicyQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = YouthPolicyController.class)
@AutoConfigureMockMvc(addFilters = false)
class YouthPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private YouthPolicyQueryService queryService;

    @MockitoBean
    private YouthPolicyIngestService ingestService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    private YouthPolicyResponse createResponse(Long id, String name) {
        return YouthPolicyResponse.builder()
                .id(id)
                .productId(1)
                .policyNo("P-" + id)
                .policyName(name)
                .categoryLarge("주거")
                .categoryMiddle("월세")
                .keyword("지원")
                .agency("서울청년센터")
                .regionCode("11000")
                .startDate("2025-01-01")
                .endDate("2025-12-31")
                .supportContent("지원 내용")
                .applyUrl("http://example.com")
                .build();
    }

    @Test
    @DisplayName("GET /api/youth-policies - 정책 목록 페이징 조회")
    void getPolicies_returnsPagedList() throws Exception {
        // given
        YouthPolicyResponse r1 = createResponse(1L, "청년 월세 지원");
        YouthPolicyResponse r2 = createResponse(2L, "청년 보증금 지원");

        Pageable pageable = PageRequest.of(0, 10);
        Page<YouthPolicyResponse> page =
                new PageImpl<>(List.of(r1, r2), pageable, 2);

        given(queryService.getPaged(
                eq("청년"),
                eq("11000"),
                any(Pageable.class),
                any(Sort.class)
        )).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/youth-policies")
                        .param("keyword", "청년")
                        .param("regionCode", "11000")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "startDate,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].policyName", is("청년 월세 지원")))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("GET /api/youth-policies/recent - 최근 30일 내 정책 조회")
    void getRecentPolicies_returnsRecentPage() throws Exception {
        // given
        YouthPolicyResponse r1 = createResponse(1L, "최근 정책 1");
        Page<YouthPolicyResponse> page =
                new PageImpl<>(List.of(r1), PageRequest.of(0, 5), 1);

        given(queryService.getRecentPolicies(any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/youth-policies/recent")
                        .param("page", "0")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].policyName", is("최근 정책 1")));
    }

    @Test
    @DisplayName("GET /api/youth-policies/closing-soon - 마감 임박 정책 조회")
    void getClosingSoonPolicies_returnsClosingSoonPage() throws Exception {
        // given
        YouthPolicyResponse r1 = createResponse(1L, "마감 임박 정책");
        Page<YouthPolicyResponse> page =
                new PageImpl<>(List.of(r1), PageRequest.of(0, 5), 1);

        given(queryService.getClosingSoonPolicies(any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/youth-policies/closing-soon")
                        .param("page", "0")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].policyName", is("마감 임박 정책")));
    }

    @Test
    @DisplayName("GET /api/youth-policies/recommend/{userId} - 사용자 맞춤 추천 정책 조회")
    void recommendPolicies_returnsRecommendedList() throws Exception {
        // given
        YouthPolicyResponse r1 = createResponse(1L, "서울 청년 월세 지원");
        YouthPolicyResponse r2 = createResponse(2L, "서울 청년 전세 지원");

        given(queryService.recommendForUser(eq(1), eq(false)))
                .willReturn(List.of(r1, r2));

        // when & then
        mockMvc.perform(get("/api/youth-policies/recommend/{userId}", 1)
                        .param("strictRegionMatch", "false")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].policyName", is("서울 청년 월세 지원")));
    }

    @Test
    @DisplayName("GET /api/youth-policies/{id} - 단일 정책 상세 조회")
    void getPolicyById_returnsSinglePolicy() throws Exception {
        // given
        YouthPolicyResponse r1 = createResponse(1L, "단일 정책");

        given(queryService.getById(1L)).willReturn(r1);

        // when & then
        mockMvc.perform(get("/api/youth-policies/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.policyName", is("단일 정책")));
    }

    @Test
    @DisplayName("POST /api/youth-policies/sync - 온통청년 정책 동기화 호출")
    void syncPolicies_callsIngestService() throws Exception {
        // given
        // ingestService.ingestAllPolicies()는 void라서 BDDMockito willDoNothing() 생략 가능

        // when & then
        mockMvc.perform(post("/api/youth-policies/sync"))
                .andExpect(status().isOk());
    }
}
