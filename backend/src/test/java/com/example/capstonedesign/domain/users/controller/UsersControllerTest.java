package com.example.capstonedesign.domain.users.controller;

import com.example.capstonedesign.common.exception.ApiException;
import com.example.capstonedesign.common.exception.ErrorCode;
import com.example.capstonedesign.domain.notifications.entity.NotificationChannel;
import com.example.capstonedesign.domain.users.config.JwtTokenProvider;
import com.example.capstonedesign.domain.users.config.PasswordEncoder;
import com.example.capstonedesign.domain.users.dto.request.LoginRequest;
import com.example.capstonedesign.domain.users.dto.request.SignupRequest;
import com.example.capstonedesign.domain.users.dto.request.UpdateUserRequest;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsersController.class)
class UsersControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Autowired
    UsersController usersController;

    @MockitoBean
    UsersService usersService;

    @MockitoBean
    PasswordEncoder passwordEncoder;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    // ----------------------------------------------------------
    // 1. 회원 가입
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    @DisplayName("회원 가입 성공")
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

        when(usersService.signup(any(SignupRequest.class))).thenReturn(res);

        mvc.perform(post("/api/users/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스터"));

        verify(usersService).signup(any(SignupRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("회원 가입 실패 - 이메일 중복(CONFLICT)")
    void signup_conflict() throws Exception {
        SignupRequest req = new SignupRequest(
                "dup@example.com", "Password1!", "테스터",
                22, "중위소득150%이하", "서울", false, true,
                UserRole.USER, LocalDate.now()
        );

        when(usersService.signup(any(SignupRequest.class)))
                .thenThrow(new ApiException(ErrorCode.CONFLICT, "이미 가입된 이메일입니다."));

        mvc.perform(post("/api//users/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // ----------------------------------------------------------
    // 2. 로그인
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
        when(jwtTokenProvider.generate(1L, "aaa@example.com", UserRole.USER))
                .thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirySeconds()).thenReturn(3600L);

        mvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    @WithMockUser
    @DisplayName("로그인 실패 → 잘못된 비밀번호(UNAUTHORIZED)")
    void login_invalid_password() throws Exception {
        LoginRequest req = new LoginRequest("aaa@example.com", "wrong");

        Users mockUser = Users.builder()
                .id(1).email("aaa@example.com").password("encoded").build();

        when(usersService.requireActiveByEmail("aaa@example.com"))
                .thenReturn(mockUser);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        mvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------
    // 3. 내 정보 조회
    // ----------------------------------------------------------
    @Test
    @DisplayName("내 정보 조회 실패 - 인증 없음(401)")
    void me_unauthorized() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1") // principal.username = "1" → ID 분기
    @DisplayName("내 정보 조회 성공")
    void me_success() throws Exception {
        Users mockUser = Users.builder()
                .id(1)
                .email("test@example.com")
                .name("테스터")
                .role(UserRole.USER)
                .build();

        UsersResponse res = new UsersResponse(
                1, "test@example.com", "테스터",
                null, null, null, null, true, null, UserRole.USER
        );

        // resolveUserFromPrincipal → requireActiveById(1)
        when(usersService.requireActiveById(1)).thenReturn(mockUser);
        when(usersService.toResponse(mockUser)).thenReturn(res);

        mvc.perform(get("/api/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    @DisplayName("내 정보 조회 성공 - principal이 이메일일 때")
    void me_withEmailPrincipal_success() throws Exception {
        Users mockUser = Users.builder()
                .id(1)
                .email("user@example.com")
                .name("테스터")
                .role(UserRole.USER)
                .build();

        UsersResponse res = new UsersResponse(
                1, "user@example.com", "테스터",
                null, null, null, null, true, null, UserRole.USER
        );

        // resolveUserFromPrincipal → UserDetails → username="user@example.com"
        when(usersService.requireActiveByEmail("user@example.com")).thenReturn(mockUser);
        when(usersService.toResponse(mockUser)).thenReturn(res);

        mvc.perform(get("/api/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    // ----------------------------------------------------------
    // 4. 내 정보 수정
    // ----------------------------------------------------------
    @Test
    @WithMockUser(username = "1") // principal.username = "1" → UserDetails → ID 분기
    @DisplayName("내 정보 수정 성공")
    void updateMe_success() throws Exception {
        Users me = Users.builder()
                .id(1)
                .email("user@example.com")
                .name("기존이름")
                .role(UserRole.USER)
                .build();

        UsersResponse updatedRes = new UsersResponse(
                1,
                "user@example.com",
                "기존이름",
                25,
                "중위소득150%이하",
                "부산",
                true,
                false,
                LocalDate.of(1999, 12, 31),
                UserRole.USER
        );

        when(usersService.requireActiveById(1)).thenReturn(me);
        when(usersService.updateProfile(eq("user@example.com"), any(UpdateUserRequest.class)))
                .thenReturn(updatedRes);

        String body = """
            {
              "age": 25,
              "income_band": "중위소득150%이하",
              "region": "부산",
              "is_homeless": true,
              "notificationEnabled": false,
              "birthdate": "1999-12-31"
            }
            """;

        mvc.perform(put("/api/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(25))
                .andExpect(jsonPath("$.region").value("부산"))
                .andExpect(jsonPath("$.notificationEnabled").value(false));
    }

    // ----------------------------------------------------------
    // 5. 알림 설정
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    @DisplayName("알림 수신 설정 변경 - enabled=true")
    void updateNotificationPreference_enabled() throws Exception {
        mvc.perform(patch("/api/users/{id}/notification", 1)
                        .with(csrf())
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string("알림 수신이 활성화되었습니다."));

        verify(usersService).updateNotificationPreference(1, true);
    }

    @Test
    @WithMockUser(username = "1") // principal.username = "1" → ID 분기
    @DisplayName("알림 채널 변경 성공")
    void updateNotificationChannel_success() throws Exception {
        Users me = Users.builder()
                .id(1)
                .email("user@example.com")
                .role(UserRole.USER)
                .build();

        when(usersService.requireActiveById(1)).thenReturn(me);

        mvc.perform(put("/api/users/notification-channel")
                        .with(csrf())
                        .param("channel", NotificationChannel.EMAIL.name()))
                .andExpect(status().isOk())
                .andExpect(content().string("알림 채널이 EMAIL(으)로 변경되었습니다."));

        verify(usersService).updateNotificationChannel(1, NotificationChannel.EMAIL);
    }

    @Test
    @WithMockUser(username = "user@example.com")
    @DisplayName("알림 채널 변경 성공 - principal이 이메일일 때")
    void updateNotificationChannel_withEmailPrincipal_success() throws Exception {
        Users me = Users.builder()
                .id(7)
                .email("user@example.com")
                .role(UserRole.USER)
                .build();

        // resolveUserFromPrincipal → UserDetails.username = "user@example.com"
        when(usersService.requireActiveByEmail("user@example.com")).thenReturn(me);

        mvc.perform(put("/api/users/notification-channel")
                        .with(csrf())
                        .param("channel", NotificationChannel.KAKAO.name()))
                .andExpect(status().isOk())
                .andExpect(content().string("알림 채널이 KAKAO(으)로 변경되었습니다."));

        verify(usersService).updateNotificationChannel(7, NotificationChannel.KAKAO);
    }

    // ----------------------------------------------------------
    // 6. 아이디(이메일) 찾기
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    @DisplayName("아이디 찾기 - 인증번호 발송 성공")
    void requestIdVerification_success() throws Exception {

        Map<String, String> mockResponse = Map.of(
                "email", "user@example.com",
                "maskedEmail", "u***r@example.com"
        );

        when(usersService.sendIdVerificationCode(
                eq("홍길동"),
                eq(LocalDate.parse("1990-01-01")),
                eq("서울특별시 강서구")
        )).thenReturn(mockResponse);

        mvc.perform(post("/api/users/find-id/request")
                        .with(csrf())
                        .param("name", "홍길동")
                        .param("birthdate", "1990-01-01")
                        .param("region", "서울특별시 강서구"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.maskedEmail").value("u***r@example.com"));

        verify(usersService).sendIdVerificationCode(
                "홍길동",
                LocalDate.parse("1990-01-01"),
                "서울특별시 강서구"
        );
    }

    @Test
    @WithMockUser
    @DisplayName("아이디 찾기 - 인증번호 확인 성공")
    void confirmIdVerification_success() throws Exception {

        when(usersService.confirmIdVerification("user@example.com", "123456"))
                .thenReturn("user@example.com");

        mvc.perform(post("/api/users/find-id/confirm")
                        .with(csrf())
                        .param("email", "user@example.com")
                        .param("code", "123456"))
                .andExpect(status().isOk())
                .andExpect(content().string("회원님의 아이디(이메일)는 user@example.com 입니다."));

        verify(usersService).confirmIdVerification("user@example.com", "123456");
    }

    // ----------------------------------------------------------
    // 7. 비밀번호 재설정
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    @DisplayName("비밀번호 재설정 메일 요청 성공")
    void requestPasswordReset_success() throws Exception {
        String body = """
            { "email": "user@example.com" }
            """;

        mvc.perform(post("/api/users/password-reset/request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("비밀번호 재설정 안내 메일이 발송되었습니다."));

        verify(usersService).requestPasswordReset("user@example.com");
    }

    @Test
    @WithMockUser
    @DisplayName("비밀번호 재설정 확정 성공")
    void confirmPasswordReset_success() throws Exception {
        String body = """
            {
              "token": "reset-token",
              "newPassword": "NewPw123!"
            }
            """;

        mvc.perform(post("/api/users/password-reset/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("비밀번호가 성공적으로 재설정되었습니다."));

        verify(usersService).confirmPasswordReset("reset-token", "NewPw123!");
    }

    // ----------------------------------------------------------
    // 8. 회원 탈퇴
    // ----------------------------------------------------------
    @Test
    @WithMockUser(username = "1") // principal.username = "1" → ID 분기
    @DisplayName("회원 탈퇴 성공")
    void delete_success() throws Exception {
        Users me = Users.builder()
                .id(1)
                .email("user@example.com")
                .role(UserRole.USER)
                .build();

        when(usersService.requireActiveById(1)).thenReturn(me);
        when(usersService.delete("user@example.com", "Pw1234!"))
                .thenReturn("회원 탈퇴가 완료되었습니다.");

        String body = """
            { "password": "Pw1234!" }
            """;

        mvc.perform(delete("/api/users/delete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("회원 탈퇴가 완료되었습니다."));

        verify(usersService).delete("user@example.com", "Pw1234!");
    }

    // ----------------------------------------------------------
    // 9-1. resolveUserFromPrincipal - principal이 Long일 때
    // ----------------------------------------------------------
    @Test
    @DisplayName("resolveUserFromPrincipal - principal이 Long이면 ID 기반 조회")
    void resolveUserFromPrincipal_longPrincipal_callsRequireActiveById() throws Exception {
        // given
        Users mockUser = Users.builder()
                .id(1)
                .email("long@example.com")
                .role(UserRole.USER)
                .build();

        // Long 1L → Integer 1로 변환 후 requireActiveById(1) 호출 기대
        when(usersService.requireActiveById(1)).thenReturn(mockUser);

        // private 메서드 리플렉션으로 꺼내기
        var method = UsersController.class
                .getDeclaredMethod("resolveUserFromPrincipal", Object.class);
        method.setAccessible(true);

        // when
        Users result = (Users) method.invoke(usersController, 1L);

        // then
        org.junit.jupiter.api.Assertions.assertEquals(mockUser, result);
        verify(usersService).requireActiveById(1);
    }

    // ----------------------------------------------------------
    // 9-2. resolveUserFromPrincipal - toString()이 숫자일 때
    // ----------------------------------------------------------
    @Test
    @DisplayName("resolveUserFromPrincipal - principal.toString()이 숫자면 ID 기반 조회")
    void resolveUserFromPrincipal_stringNumber_callsRequireActiveById() throws Exception {
        // given: Long/String/UserDetails가 아닌, toString()만 숫자인 객체
        Object numericPrincipal = new Object() {
            @Override
            public String toString() {
                return "42";
            }
        };

        Users mockUser = Users.builder()
                .id(42)
                .email("id42@example.com")
                .role(UserRole.USER)
                .build();

        when(usersService.requireActiveById(42)).thenReturn(mockUser);

        var method = UsersController.class
                .getDeclaredMethod("resolveUserFromPrincipal", Object.class);
        method.setAccessible(true);

        // when
        Users result = (Users) method.invoke(usersController, numericPrincipal);

        // then
        org.junit.jupiter.api.Assertions.assertEquals(mockUser, result);
        verify(usersService).requireActiveById(42);
    }

    // ----------------------------------------------------------
    // 9-3. resolveUserFromPrincipal - toString()이 이메일일 때
    // ----------------------------------------------------------
    @Test
    @DisplayName("resolveUserFromPrincipal - principal.toString()이 이메일이면 이메일 기반 조회")
    void resolveUserFromPrincipal_stringEmail_callsRequireActiveByEmail() throws Exception {
        // given: 숫자가 아닌 이메일 문자열을 반환하는 객체
        Object emailPrincipal = new Object() {
            @Override
            public String toString() {
                return "fallback@example.com";
            }
        };

        Users mockUser = Users.builder()
                .id(7)
                .email("fallback@example.com")
                .role(UserRole.USER)
                .build();

        when(usersService.requireActiveByEmail("fallback@example.com"))
                .thenReturn(mockUser);

        var method = UsersController.class
                .getDeclaredMethod("resolveUserFromPrincipal", Object.class);
        method.setAccessible(true);

        // when
        Users result = (Users) method.invoke(usersController, emailPrincipal);

        // then
        org.junit.jupiter.api.Assertions.assertEquals(mockUser, result);
        verify(usersService).requireActiveByEmail("fallback@example.com");
    }
}
