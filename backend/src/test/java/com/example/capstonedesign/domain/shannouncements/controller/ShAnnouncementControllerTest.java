package com.example.capstonedesign.domain.shannouncements.controller;

import com.example.capstonedesign.domain.shannouncements.dto.response.ShAnnouncementResponse;
import com.example.capstonedesign.domain.shannouncements.service.ShAnnouncementService;
import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ShAnnouncementControllerTest
 * -----------------------------------------------------
 * - SH 공고 조회/검색/추천 공개 API 테스트
 * - Security 필터는 비활성화(addFilters = false) 해서 401 방지
 */
@WebMvcTest(controllers = ShAnnouncementController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShAnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShAnnouncementService shAnnouncementService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    /** 공통으로 사용할 샘플 응답 DTO */
    private ShAnnouncementResponse sampleResponse() {
        return ShAnnouncementResponse.builder()
                .id(1L)
                .productId(100)
                .title("청년안심주택 A단지 모집공고")
                .department("청년주거지원팀")
                .postDate(LocalDate.now())
                .views(123)
                .recruitStatus("now")
                .supplyType("청년안심주택")
                .attachments(List.of(
                        Map.of("name", "공고문", "url", "https://example.com/file1.pdf")
                ))
                .score(10.5)
                .reason("거주 지역과 동일 지역 공고, 청년층 우대 대상")
                .build();
    }

    @Test
    @DisplayName("GET /api/sh/housings - SH 공고 전체 조회 API")
    void getAll() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<ShAnnouncementResponse> content = List.of(sampleResponse());
        Page<ShAnnouncementResponse> page = new PageImpl<>(content, pageable, content.size());

        given(shAnnouncementService.getAll(any(Pageable.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/sh/housings")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("청년안심주택 A단지 모집공고"))
                .andExpect(jsonPath("$.content[0].supplyType").value("청년안심주택"));
    }

    @Test
    @DisplayName("GET /api/sh/housings/search - 조건 검색 API (키워드만)")
    void search() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<ShAnnouncementResponse> content = List.of(sampleResponse());
        Page<ShAnnouncementResponse> page = new PageImpl<>(content, pageable, content.size());

        given(shAnnouncementService.search(
                any(),              // SHHousingCategory category (null 포함)
                any(),              // RecruitStatus status (null 포함)
                eq("청년"),          // keyword
                any(Pageable.class)
        )).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/sh/housings/search")
                        .param("keyword", "청년")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("청년안심주택 A단지 모집공고"));
    }

    @Test
    @DisplayName("GET /api/sh/housings/recent - 최근 7일 공고 조회 API")
    void recent() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<ShAnnouncementResponse> content = List.of(sampleResponse());
        Page<ShAnnouncementResponse> page = new PageImpl<>(content, pageable, content.size());

        given(shAnnouncementService.getRecent(any(Pageable.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/sh/housings/recent")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("청년안심주택 A단지 모집공고"));
    }

    @Test
    @DisplayName("GET /api/sh/housings/recommend - 청년 친화형 공고 추천 (지역 필터 포함)")
    void recommendByRegion() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<ShAnnouncementResponse> content = List.of(sampleResponse());
        Page<ShAnnouncementResponse> page = new PageImpl<>(content, pageable, content.size());

        given(shAnnouncementService.getYouthRecommendations(eq("강남"), any(Pageable.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/sh/housings/recommend")
                        .param("region", "강남")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("청년안심주택 A단지 모집공고"))
                .andExpect(jsonPath("$.content[0].reason").exists());
    }

    @Test
    @DisplayName("GET /api/sh/housings/recommend/{userId} - 사용자 맞춤 SH 공고 추천 API")
    void recommendForUser() throws Exception {
        // given
        List<ShAnnouncementResponse> recommendations = List.of(sampleResponse());

        given(shAnnouncementService.recommendForUser(eq(1), eq(false)))
                .willReturn(recommendations);

        // when & then
        mockMvc.perform(get("/api/sh/housings/recommend/{userId}", 1)
                        .param("strictRegionMatch", "false")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("청년안심주택 A단지 모집공고"))
                .andExpect(jsonPath("$[0].score").value(10.5));
    }

    @Test
    @DisplayName("GET /api/sh/housings/recommend - 지역 파라미터 없이 호출 시 전체 추천 API를 사용한다")
    void recommendWithoutRegion() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<ShAnnouncementResponse> content = List.of(sampleResponse());
        Page<ShAnnouncementResponse> page = new PageImpl<>(content, pageable, content.size());

        // region이 비어 있을 때 호출되는 오버로드 버전
        given(shAnnouncementService.getYouthRecommendations(any(Pageable.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/sh/housings/recommend")
                        // ★ region 파라미터를 일부러 안 보냄
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("청년안심주택 A단지 모집공고"))
                .andExpect(jsonPath("$.content[0].supplyType").value("청년안심주택"));
    }
}
