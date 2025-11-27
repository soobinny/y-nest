package com.example.capstonedesign.domain.users.service;

import com.example.capstonedesign.common.exception.ApiException;
import com.example.capstonedesign.common.exception.ErrorCode;
import com.example.capstonedesign.domain.notifications.entity.NotificationChannel;
import com.example.capstonedesign.domain.users.config.PasswordEncoder;
import com.example.capstonedesign.domain.users.dto.request.SignupRequest;
import com.example.capstonedesign.domain.users.dto.request.UpdateUserRequest;
import com.example.capstonedesign.domain.users.dto.response.UsersResponse;
import com.example.capstonedesign.domain.users.entity.PasswordResetToken;
import com.example.capstonedesign.domain.users.entity.UserRole;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.port.EmailSender;
import com.example.capstonedesign.domain.users.repository.PasswordResetTokenRepository;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * UsersService 단위 테스트
 * - Repository / Encoder / EmailSender 등을 Mocking
 * - 각 메서드별 핵심 시나리오를 1~2개씩 검증
 */
@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock
    UsersRepository usersRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    PasswordResetTokenRepository prtRepository;

    @Mock
    EmailSender emailSender;

    @InjectMocks
    UsersService usersService;

    private Users activeUser;

    @BeforeEach
    void setUp() {
        activeUser = Users.builder()
                .id(1)
                .email("test@example.com")
                .password("encoded-pass")
                .name("테스터")
                .age(25)
                .income_band("중위소득100%이하")
                .region("서울")
                .is_homeless(false)
                .birthdate(LocalDate.of(2000, 1, 1))
                .role(UserRole.USER)
                .deleted(false)
                .created_at(Instant.now())
                .updated_at(Instant.now())
                .notificationEnabled(true)
                .notificationChannel(NotificationChannel.EMAIL)
                .build();
    }

    // -------------------------------------------------------------------------
    // 1. signup()
    // -------------------------------------------------------------------------
    @Test
    void signup() {
        // given
        SignupRequest req = new SignupRequest(
                "new@example.com",
                "raw-pass",
                "새유저",
                22,
                "중위소득200%이하",
                "부산",
                false,
                true,
                UserRole.USER,
                LocalDate.of(2003, 3, 3)
        );

        when(usersRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode("raw-pass")).thenReturn("encoded-pass");

        // save 호출 시, 저장될 Users를 그대로 반환하도록 설정
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> {
            Users u = invocation.getArgument(0);
            u.setId(10);
            return u;
        });

        // when
        UsersResponse res = usersService.signup(req);

        // then
        assertNotNull(res);
        assertEquals("new@example.com", res.email());
        assertEquals("새유저", res.name());
        verify(usersRepository, times(1)).existsByEmail("new@example.com");
        verify(usersRepository, times(1)).save(any(Users.class));

        // 중복 이메일 시나리오도 같은 테스트에서 한 번 더 검증
        when(usersRepository.existsByEmail(req.email())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> usersService.signup(req));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // 2. requireActiveByEmail()
    // -------------------------------------------------------------------------
    @Test
    void requireActiveByEmail() {
        // 성공 시나리오
        when(usersRepository.findByEmailAndDeletedFalse("test@example.com"))
                .thenReturn(Optional.of(activeUser));

        Users found = usersService.requireActiveByEmail("test@example.com");
        assertNotNull(found);
        assertEquals(activeUser.getId(), found.getId());

        // 실패 시나리오: 없는 사용자
        when(usersRepository.findByEmailAndDeletedFalse("no@example.com"))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> usersService.requireActiveByEmail("no@example.com"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // 3. requireActiveById()
    // -------------------------------------------------------------------------
    @Test
    void requireActiveById() {
        // 성공 시나리오
        when(usersRepository.findById(1)).thenReturn(Optional.of(activeUser));

        Users found = usersService.requireActiveById(1);
        assertNotNull(found);
        assertEquals(1, found.getId());

        // 실패 시나리오: deleted=true
        Users deletedUser = Users.builder()
                .id(2)
                .email("deleted@example.com")
                .deleted(true)
                .build();

        when(usersRepository.findById(2)).thenReturn(Optional.of(deletedUser));

        ApiException ex = assertThrows(ApiException.class,
                () -> usersService.requireActiveById(2));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // 4. sendIdVerificationCode() 테스트
    // -------------------------------------------------------------------------
    @Test
    void confirmIdVerification_success() throws Exception {
        String email = "test@example.com";
        String name = "테스터";
        LocalDate birthdate = LocalDate.of(1990,1,1);
        String region = "서울특별시 강서구";

        Users user = Users.builder()
                .email(email)
                .name(name)
                .birthdate(birthdate)
                .region(region)
                .build();

        when(usersRepository.findByNameAndBirthdateAndRegionAndDeletedFalse(
                name, birthdate, region
        )).thenReturn(Optional.of(user));

        // 1) 인증번호 저장
        usersService.sendIdVerificationCode(name, birthdate, region);

        // 2) verificationCodes Map 읽기
        Field field = UsersService.class.getDeclaredField("verificationCodes");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) field.get(usersService);

        Object vc = map.get(email);
        assertNotNull(vc, "verificationCodes에 값이 있어야 한다.");

        // 3) 내부 code 필드 추출
        Field codeField = vc.getClass().getDeclaredField("code");
        codeField.setAccessible(true);
        String storedCode = (String) codeField.get(vc);

        // 4) confirm 검증
        String resultEmail = usersService.confirmIdVerification(email, storedCode);

        assertEquals(email, resultEmail);
    }

    // -------------------------------------------------------------------------
    // 5. confirmIdVerification()
    // -------------------------------------------------------------------------
    @Test
    void confirmIdVerification() throws Exception {
        // given
        String email = "test@example.com";
        String name = "테스터";
        LocalDate birthdate = LocalDate.of(1990, 1, 1);
        String region = "서울특별시 강서구";

        Users user = Users.builder()
                .email(email)
                .name(name)
                .birthdate(birthdate)
                .region(region)
                .build();

        // sendIdVerificationCode() 가 사용자 조회할 수 있도록 mock 설정
        when(usersRepository.findByNameAndBirthdateAndRegionAndDeletedFalse(
                name, birthdate, region
        )).thenReturn(Optional.of(user));

        // (1) 먼저 sendIdVerificationCode 호출 → verificationCodes 맵에 값 들어감
        usersService.sendIdVerificationCode(name, birthdate, region);

        // (2) Reflection으로 private Map<String, VerificationCode> 접근
        Field field = UsersService.class.getDeclaredField("verificationCodes");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) field.get(usersService);

        Object vc = map.get(email);
        assertNotNull(vc, "verificationCodes에 값이 있어야 한다.");

        // VerificationCode 내부 code 필드 읽기
        Field codeField = vc.getClass().getDeclaredField("code");
        codeField.setAccessible(true);
        String storedCode = (String) codeField.get(vc);

        // (3) confirm 검증
        String resultEmail = usersService.confirmIdVerification(email, storedCode);

        // then
        assertEquals(email, resultEmail);

        // (4) 실패 시나리오: 잘못된 이메일
        ApiException ex = assertThrows(ApiException.class,
                () -> usersService.confirmIdVerification("none@example.com", "123456"));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // 6. requestPasswordReset()
    // -------------------------------------------------------------------------
    @Test
    void requestPasswordReset() {
        // 존재하는 사용자
        when(usersRepository.findByEmailAndDeletedFalse("test@example.com"))
                .thenReturn(Optional.of(activeUser));

        usersService.requestPasswordReset("test@example.com");

        // 토큰 저장 + 메일 전송 확인
        verify(prtRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailSender, times(1))
                .send(eq("test@example.com"), contains("비밀번호 재설정"), anyString());

        // 존재하지 않는 사용자 (Optional.empty)여도 예외 없이 그냥 무시하는 설계
        when(usersRepository.findByEmailAndDeletedFalse("no@example.com"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> usersService.requestPasswordReset("no@example.com"));
    }

    // -------------------------------------------------------------------------
    // 7. confirmPasswordReset()
    // -------------------------------------------------------------------------
    @Test
    void confirmPasswordReset() {
        String token = "reset-token";

        PasswordResetToken prt = new PasswordResetToken();
        prt.setId(1L);
        prt.setUserId(activeUser.getId());
        prt.setToken(token);
        prt.setExpiresAt(Instant.now().plusSeconds(60)); // 아직 유효
        prt.setUsed(false);

        when(prtRepository.findByToken(token)).thenReturn(Optional.of(prt));
        when(usersRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");

        // when
        usersService.confirmPasswordReset(token, "new-pass");

        // then
        assertEquals("encoded-new", activeUser.getPassword());
        assertTrue(prt.isUsed());

        // 만료/이미 사용된 토큰 시나리오
        PasswordResetToken expired = new PasswordResetToken();
        expired.setId(2L);
        expired.setUserId(activeUser.getId());
        expired.setToken("expired");
        expired.setExpiresAt(Instant.now().minusSeconds(10));
        expired.setUsed(false);

        when(prtRepository.findByToken("expired")).thenReturn(Optional.of(expired));

        ApiException ex1 = assertThrows(ApiException.class,
                () -> usersService.confirmPasswordReset("expired", "pw"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex1.getErrorCode());

        PasswordResetToken used = new PasswordResetToken();
        used.setId(3L);
        used.setUserId(activeUser.getId());
        used.setToken("used");
        used.setExpiresAt(Instant.now().plusSeconds(60));
        used.setUsed(true);

        when(prtRepository.findByToken("used")).thenReturn(Optional.of(used));

        ApiException ex2 = assertThrows(ApiException.class,
                () -> usersService.confirmPasswordReset("used", "pw"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex2.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // 8. updateProfile()
    // -------------------------------------------------------------------------
    @Test
    void updateProfile() {
        when(usersRepository.findByEmailAndDeletedFalse("test@example.com"))
                .thenReturn(Optional.of(activeUser));

        UpdateUserRequest req = new UpdateUserRequest(
                30,              // age
                "중위소득150%이하",         // income_band
                "인천",          // region
                true,            // is_homeless
                false,           // notificationEnabled
                LocalDate.of(1999, 12, 31) // birthdate
        );

        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsersResponse res = usersService.updateProfile("test@example.com", req);

        assertEquals(30, res.age());
        assertEquals("중위소득150%이하", res.income_band());
        assertEquals("인천", res.region());
        assertTrue(res.is_homeless());
        assertFalse(res.notificationEnabled());
        assertEquals(LocalDate.of(1999, 12, 31), res.birthdate());
    }

    // -------------------------------------------------------------------------
    // 9. updateNotificationPreference()
    // -------------------------------------------------------------------------
    @Test
    void updateNotificationPreference() {
        when(usersRepository.findById(1)).thenReturn(Optional.of(activeUser));

        usersService.updateNotificationPreference(1, false);

        assertFalse(activeUser.getNotificationEnabled());
        verify(usersRepository, times(1)).save(activeUser);
    }

    // -------------------------------------------------------------------------
    // 10. updateNotificationChannel()
    // -------------------------------------------------------------------------
    @Test
    void updateNotificationChannel() {
        when(usersRepository.findById(1)).thenReturn(Optional.of(activeUser));

        usersService.updateNotificationChannel(1, NotificationChannel.SMS);

        assertEquals(NotificationChannel.SMS, activeUser.getNotificationChannel());
        verify(usersRepository, times(1)).save(activeUser);
    }

    // -------------------------------------------------------------------------
    // 11. delete()
    // -------------------------------------------------------------------------
    @Test
    void delete() {
        when(usersRepository.findByEmailAndDeletedFalse("test@example.com"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("raw-pass", "encoded-pass")).thenReturn(true);

        String msg = usersService.delete("test@example.com", "raw-pass");

        assertTrue(activeUser.getDeleted());
        assertNotNull(activeUser.getDeleted_at());
        assertEquals("회원 탈퇴가 완료되었습니다.", msg);

        // 비밀번호 불일치 시나리오
        when(passwordEncoder.matches("wrong", "encoded-pass")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> usersService.delete("test@example.com", "wrong"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // 12. toResponse()
    // -------------------------------------------------------------------------
    @Test
    void toResponse() {
        UsersResponse res = usersService.toResponse(activeUser);

        assertEquals(activeUser.getId(), res.id());
        assertEquals(activeUser.getEmail(), res.email());
        assertEquals(activeUser.getName(), res.name());
        assertEquals(activeUser.getAge(), res.age());
        assertEquals(activeUser.getIncome_band(), res.income_band());
        assertEquals(activeUser.getRegion(), res.region());
        assertEquals(activeUser.getIs_homeless(), res.is_homeless());
        assertEquals(activeUser.getNotificationEnabled(), res.notificationEnabled());
        assertEquals(activeUser.getBirthdate(), res.birthdate());
        assertEquals(activeUser.getRole(), res.role());
    }
}
