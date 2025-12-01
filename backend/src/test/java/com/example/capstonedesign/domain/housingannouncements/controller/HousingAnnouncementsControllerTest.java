package com.example.capstonedesign.domain.housingannouncements.controller;

import com.example.capstonedesign.domain.housingannouncements.dto.response.HousingAnnouncementsResponse;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingStatus;
import com.example.capstonedesign.domain.housingannouncements.service.HousingAnnouncementsService;
import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HousingAnnouncementsController.class)
@Import(HousingAnnouncementsControllerTest.TestSecurityConfig.class)
class HousingAnnouncementsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HousingAnnouncementsService service;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    private HousingAnnouncementsResponse createResponse(long id, String name) {
        return HousingAnnouncementsResponse.builder()
                .id(id)
                .productId(1)
                .name(name)
                .provider("LH 한국토지주택공사")
                .regionName("서울특별시")
                .noticeDate(LocalDate.of(2025, 11, 20))
                .closeDate(LocalDate.of(2025, 11, 30))
                .status(HousingStatus.공고중)
                .category(HousingCategory.임대주택)
                .detailUrl("https://example.com/" + id)
                .build();
    }

    @Test
    @DisplayName("GET /api/housings - 전체 공고 조회 API")
    void getAll() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("noticeDate").descending());
        Page<HousingAnnouncementsResponse> page =
                new PageImpl<>(List.of(createResponse(1L, "행복주택 A")), pageable, 1);

        given(service.getAll(any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/housings")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("행복주택 A"));
    }

    @Test
    @DisplayName("GET /api/housings/search - 조건 검색 API")
    void search() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<HousingAnnouncementsResponse> page =
                new PageImpl<>(List.of(createResponse(1L, "서울 행복주택")), pageable, 1);

        given(service.search(
                any(HousingCategory.class),
                any(HousingStatus.class),
                anyString(),
                any(Pageable.class)
        )).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/housings/search")
                        .param("category", "임대주택")
                        .param("status", "공고중")
                        .param("region", "서울"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("서울 행복주택"));

        Mockito.verify(service).search(
                eq(HousingCategory.임대주택),
                eq(HousingStatus.공고중),
                eq("서울"),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/housings/closing-soon - 마감 임박 공고 조회 API")
    void closingSoon() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<HousingAnnouncementsResponse> page =
                new PageImpl<>(List.of(createResponse(1L, "임박 공고")), pageable, 1);

        given(service.getClosingSoon(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/housings/closing-soon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("임박 공고"));
    }

    @Test
    @DisplayName("GET /api/housings/recent - 최근 공고 조회 API")
    void recent() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<HousingAnnouncementsResponse> page =
                new PageImpl<>(List.of(createResponse(1L, "최근 공고")), pageable, 1);

        given(service.getRecent(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/housings/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("최근 공고"));
    }

    @Test
    @DisplayName("GET /api/housings/recommend/{userId} - 사용자 맞춤 추천 API")
    void recommendForUser() throws Exception {
        List<HousingAnnouncementsResponse> list =
                List.of(createResponse(1L, "추천 공고 A"));

        given(service.recommendForUser(eq(1), eq(false))).willReturn(list);

        mockMvc.perform(get("/api/housings/recommend/{userId}", 1)
                        .param("strictRegionMatch", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("추천 공고 A"));
    }
}
