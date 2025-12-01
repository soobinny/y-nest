package com.example.capstonedesign.application.ingest.Finance;

import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinlifeIngestController.class)
@AutoConfigureMockMvc
class FinlifeIngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinlifeIngestService finlifeIngestService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @WithMockUser(username = "admin", roles = "ADMIN") // Security 설정에 맞게 조정
    @Test
    void ingestCompanies_callsServiceWithDefaultMaxPages_andReturnsCount() throws Exception {
        // given: 서비스가 42건 upsert했다고 가정
        when(finlifeIngestService.syncCompanies(5)).thenReturn(42);

        // when & then
        mockMvc.perform(post("/admin/ingest/finlife/companies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)) // @RequestParam 기반
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("companies upserted: 42")));

        // service.syncCompanies(기본값 5)로 호출됐는지 검증
        verify(finlifeIngestService).syncCompanies(eq(5));
    }

    @WithMockUser(username = "admin", roles = "ADMIN")
    @Test
    void ingestProducts_callsServiceWithCustomMaxPages_andReturnsCount() throws Exception {
        when(finlifeIngestService.syncDepositAndSaving(20)).thenReturn(10);

        mockMvc.perform(post("/admin/ingest/finlife/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("maxPages", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("finance_products upserted: 10")));

        verify(finlifeIngestService).syncDepositAndSaving(eq(20));
    }

    @WithMockUser(username = "admin", roles = "ADMIN")
    @Test
    void ingestAll_callsBothCompanyAndProductSync_andReturnsCombinedMessage() throws Exception {
        when(finlifeIngestService.syncCompanies(3)).thenReturn(3);
        when(finlifeIngestService.syncDepositAndSaving(7)).thenReturn(15);

        mockMvc.perform(post("/admin/ingest/finlife/all")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("companyPages", "3")
                        .param("productPages", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("companies: 3")))
                .andExpect(content().string(containsString("products: 15")));

        verify(finlifeIngestService).syncCompanies(eq(3));
        verify(finlifeIngestService).syncDepositAndSaving(eq(7));
    }

    @WithMockUser(username = "admin", roles = "ADMIN")
    @Test
    void ingestLoans_callsServiceAndReturnsCount() throws Exception {
        when(finlifeIngestService.syncLoans(10)).thenReturn(5);

        mockMvc.perform(post("/admin/ingest/finlife/loans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("loan products upserted: 5")));

        verify(finlifeIngestService).syncLoans(eq(10));
    }
}
