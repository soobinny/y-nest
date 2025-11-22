package com.example.capstonedesign.domain.users.controller;

import com.example.capstonedesign.common.exception.ApiException;
import com.example.capstonedesign.common.exception.ErrorCode;
import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import com.example.capstonedesign.domain.users.config.PasswordEncoder;
import com.example.capstonedesign.domain.users.dto.request.LoginRequest;
import com.example.capstonedesign.domain.users.dto.request.SignupRequest;
import com.example.capstonedesign.domain.users.dto.response.UsersResponse;
import com.example.capstonedesign.domain.users.entity.UserRole;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.service.UsersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(UsersController.class)
class UsersControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    UsersService usersService;

    @MockitoBean
    PasswordEncoder passwordEncoder;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;


    // ----------------------------------------------------------
    // 1. 회원가입 성공
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {

        SignupRequest req = new SignupRequest(
                "test@example.com",
                "Password1!",
                "테스터",
                22,
                "중위소득150%이하",
                "서울",
                false,
                true,
                UserRole.USER,
                LocalDate.of(2003, 1, 1)
        );

        UsersResponse res = new UsersResponse(
                1,
                req.email(),
                req.name(),
                req.age(),
                req.income_band(),
                req.region(),
                req.is_homeless(),
                req.notificationEnabled(),
                req.birthdate(),
                req.role()
        );

        when(usersService.signup(any())).thenReturn(res);

        mvc.perform(post("/users/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ----------------------------------------------------------
    // 2. 회원가입 - 이메일 중복
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_conflict() throws Exception {

        SignupRequest req = new SignupRequest(
                "dup@example.com", "Password1!", "테스터",
                22, "중위소득150%이하", "서울", false, true,
                UserRole.USER, LocalDate.now()
        );

        when(usersService.signup(any()))
                .thenThrow(new ApiException(ErrorCode.CONFLICT, "이미 가입된 이메일입니다."));

        mvc.perform(post("/users/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // ----------------------------------------------------------
    // 3. 로그인 성공
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    @DisplayName("로그인 성공 → JWT 발급")
    void login_success() throws Exception {

        LoginRequest req = new LoginRequest("aaa@example.com", "pw");

        Users mockUser = Users.builder()
                .id(1)
                .email("aaa@example.com")
                .password("encoded")
                .role(UserRole.USER)
                .build();

        when(usersService.requireActiveByEmail("aaa@example.com"))
                .thenReturn(mockUser);
        when(passwordEncoder.matches("pw", "encoded")).thenReturn(true);
        when(jwtTokenProvider.generate(anyLong(), eq("aaa@example.com"), eq(UserRole.USER)))
                .thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirySeconds()).thenReturn(3600L);

        mvc.perform(post("/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ----------------------------------------------------------
    // 4. 로그인 실패 (비밀번호 틀림)
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    @DisplayName("로그인 실패 → 잘못된 비밀번호")
    void login_invalid_password() throws Exception {

        LoginRequest req = new LoginRequest("aaa@example.com", "wrong");

        Users mockUser = Users.builder()
                .id(1).email("aaa@example.com").password("encoded").build();

        when(usersService.requireActiveByEmail("aaa@example.com"))
                .thenReturn(mockUser);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        mvc.perform(post("/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------
    // 5. JWT 없이 내 정보 조회 → 401
    // ----------------------------------------------------------
    @Test
    @DisplayName("내 정보 조회 실패 - 인증 없음")
    void me_unauthorized() throws Exception {

        mvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------
    // 6. JWT 인증된 내 정보 조회 → 200
    // ----------------------------------------------------------
    @Test
    @WithMockUser(username = "1")  // principal = 1
    @DisplayName("내 정보 조회 성공")
    void me_success() throws Exception {

        Users mockUser = Users.builder()
                .id(1)
                .email("test@example.com")
                .name("테스터")
                .build();

        UsersResponse res = new UsersResponse(
                1, "test@example.com", "테스터",
                null, null, null, null, true, null, UserRole.USER
        );

        when(usersService.requireActiveById(1)).thenReturn(mockUser);
        when(usersService.toResponse(mockUser)).thenReturn(res);

        mvc.perform(get("/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))  // ★ import 문제 해결됨
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
