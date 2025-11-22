package com.example.capstonedesign.domain.favorites.controller;

import com.example.capstonedesign.domain.favorites.config.CurrentUser;
import com.example.capstonedesign.domain.favorites.dto.FavoritesDto;
import com.example.capstonedesign.domain.favorites.service.FavoritesService;
import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = FavoritesController.class)
class FavoritesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FavoritesService favoritesService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    private FavoritesDto.ItemResponse createItemResponse() {
        return FavoritesDto.ItemResponse.builder()
                .productId(10L)
                .productName("테스트 상품")
                .provider("테스트 기관")
                .detailUrl("https://example.com/detail")
                .createdAt(LocalDateTime.of(2025, 11, 22, 12, 0))
                .productType(com.example.capstonedesign.domain.products.entity.ProductType.HOUSING)
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/favorites - 즐겨찾기 추가 시 201 반환")
    void add_returns_201() throws Exception {
        try (MockedStatic<CurrentUser> currentUser = Mockito.mockStatic(CurrentUser.class)) {
            currentUser.when(() -> CurrentUser.id(any())).thenReturn(1L);

            String body = """
                { "productId": 10 }
                """;

            mockMvc.perform(post("/api/favorites")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());

            verify(favoritesService).add(1L, 10L);
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/favorites/{productId} - 즐겨찾기 삭제 시 204 반환")
    void remove_returns_204() throws Exception {
        try (MockedStatic<CurrentUser> currentUser = Mockito.mockStatic(CurrentUser.class)) {
            currentUser.when(() -> CurrentUser.id(any())).thenReturn(1L);

            mockMvc.perform(delete("/api/favorites/{productId}", 10L)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(favoritesService).remove(1L, 10L);
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/favorites/toggle/{productId} - 토글 결과(boolean)를 반환한다")
    void toggle_returns_boolean() throws Exception {
        try (MockedStatic<CurrentUser> currentUser = Mockito.mockStatic(CurrentUser.class)) {
            currentUser.when(() -> CurrentUser.id(any())).thenReturn(1L);
            when(favoritesService.toggle(1L, 10L)).thenReturn(true);

            mockMvc.perform(post("/api/favorites/toggle/{productId}", 10L)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            verify(favoritesService).toggle(1L, 10L);
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/favorites - 즐겨찾기 목록 페이지를 반환한다")
    void list_returns_page() throws Exception {
        try (MockedStatic<CurrentUser> currentUser = Mockito.mockStatic(CurrentUser.class)) {
            currentUser.when(() -> CurrentUser.id(any())).thenReturn(1L);

            Pageable pageable = PageRequest.of(0, 20);
            Page<FavoritesDto.ItemResponse> page =
                    new PageImpl<>(List.of(createItemResponse()), pageable, 1);

            when(favoritesService.list(eq(1L), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/favorites")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].productId").value(10L))
                    .andExpect(jsonPath("$.content[0].productName").value("테스트 상품"));

            verify(favoritesService).list(eq(1L), any(Pageable.class));
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/favorites/exists/{productId} - 즐겨찾기 여부를 true/false로 반환한다")
    void exists_returns_boolean() throws Exception {
        try (MockedStatic<CurrentUser> currentUser = Mockito.mockStatic(CurrentUser.class)) {
            currentUser.when(() -> CurrentUser.id(any())).thenReturn(1L);
            when(favoritesService.exists(1L, 10L)).thenReturn(true);

            mockMvc.perform(get("/api/favorites/exists/{productId}", 10L))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            verify(favoritesService).exists(1L, 10L);
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/favorites - productId 누락 시 400 Bad Request")
    void add_returns_400_when_productId_missing() throws Exception {
        try (MockedStatic<CurrentUser> currentUser = Mockito.mockStatic(CurrentUser.class)) {
            currentUser.when(() -> CurrentUser.id(any())).thenReturn(1L);

            String body = "{}"; // productId 없음

            mockMvc.perform(post("/api/favorites")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}
