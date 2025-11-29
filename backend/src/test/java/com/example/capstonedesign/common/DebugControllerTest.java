package com.example.capstonedesign.common;

import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DebugController 단위(슬라이스) 테스트
 * - 실제 DB 연결 없이 DataSource/Connection/MetaData를 Mock으로 대체
 * - /admin/debug/datasource 엔드포인트가 올바른 JSON을 반환하는지 검증
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DebugController.class)
class DebugControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /admin/debug/datasource - DB 메타데이터를 JSON으로 반환")
    void ds_returnsDatasourceMetaData() throws Exception {
        // given
        String expectedUrl = "jdbc:mysql://localhost:3306/youth";
        String expectedUser = "admin";
        String expectedCatalog = "youth";

        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn(expectedUrl);
        when(metaData.getUserName()).thenReturn(expectedUser);
        when(connection.getCatalog()).thenReturn(expectedCatalog);

        // when & then
        mockMvc.perform(get("/admin/debug/datasource"))
                .andExpect(status().isOk())
                // JSON 필드 검증
                .andExpect(jsonPath("$.url").value(expectedUrl))
                .andExpect(jsonPath("$.user").value(expectedUser))
                // 필드명이 'catalog(database)' 이라서 bracket 표기 사용
                .andExpect(jsonPath("$['catalog(database)']").value(expectedCatalog));

        // try-with-resources 로 인해 close()가 호출되었는지까지 확인 (선택)
        verify(connection, times(1)).close();
    }
}
