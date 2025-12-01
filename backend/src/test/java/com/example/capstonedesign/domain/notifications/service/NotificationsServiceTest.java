package com.example.capstonedesign.domain.notifications.service;

import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceLoanOptionRepository;
import com.example.capstonedesign.domain.housingannouncements.repository.LhNoticeRepository;
import com.example.capstonedesign.domain.notifications.entity.Notifications;
import com.example.capstonedesign.domain.notifications.repository.NotificationsRepository;
import com.example.capstonedesign.domain.shannouncements.repository.ShAnnouncementRepository;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.port.EmailSender;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import com.example.capstonedesign.domain.youthpolicies.repository.YouthPolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationsServiceTest {

    @Mock
    UsersRepository usersRepository;

    @Mock
    LhNoticeRepository lhNoticeRepository;

    @Mock
    ShAnnouncementRepository shAnnouncementRepository;

    @Mock
    FinanceLoanOptionRepository loanOptionRepository;

    @Mock
    NotificationsRepository notificationsRepository;

    @Mock
    YouthPolicyRepository youthPolicyRepository;

    @Mock
    EmailSender emailSender;

    @InjectMocks
    NotificationsService notificationsService;

    @Test
    void sendDailyDigest_sendsEmailToActiveUsersOnly() {
        // given
        Users activeUser = Users.builder()
                .id(1)
                .email("active@y-nest.com")
                .deleted(false)
                .notificationEnabled(true)
                .build();

        Users deletedUser = Users.builder()
                .id(2)
                .email("deleted@y-nest.com")
                .deleted(true)
                .notificationEnabled(true)
                .build();

        Users disabledUser = Users.builder()
                .id(3)
                .email("disabled@y-nest.com")
                .deleted(false)
                .notificationEnabled(false)
                .build();

        when(usersRepository.findAll())
                .thenReturn(List.of(activeUser, deletedUser, disabledUser));

        // 섹션에서 사용하는 레포지토리들은 비어있는 리스트 반환해도 무방
        when(lhNoticeRepository.findAll()).thenReturn(Collections.emptyList());
        when(shAnnouncementRepository.findAll()).thenReturn(Collections.emptyList());
        when(loanOptionRepository.findAll()).thenReturn(Collections.emptyList());
        when(youthPolicyRepository.findAll()).thenReturn(Collections.emptyList());

        // Notifications 저장 시 그대로 반환
        when(notificationsRepository.save(any(Notifications.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        notificationsService.sendDailyDigest();

        // then
        // 1) activeUser 에게만 이메일이 발송되어야 함
        verify(emailSender, times(1))
                .sendHtml(eq("active@y-nest.com"), anyString(), anyString());

        verify(emailSender, never())
                .sendHtml(eq("deleted@y-nest.com"), anyString(), anyString());

        verify(emailSender, never())
                .sendHtml(eq("disabled@y-nest.com"), anyString(), anyString());

        // 2) Notifications 엔티티 저장 값 검증
        ArgumentCaptor<Notifications> captor = ArgumentCaptor.forClass(Notifications.class);
        verify(notificationsRepository, times(1)).save(captor.capture());

        Notifications saved = captor.getValue();
        assertEquals(activeUser, saved.getUser());
        assertEquals("EMAIL", saved.getType());
        assertEquals("SENT", saved.getStatus());
        assertNotNull(saved.getMessage());
        assertFalse(saved.getMessage().isBlank());
    }

    @Test
    void sendDailyDigest_marksNotificationFailedWhenEmailError() {
        // given
        Users user = Users.builder()
                .id(1)
                .email("error@y-nest.com")
                .deleted(false)
                .notificationEnabled(true)
                .build();

        when(usersRepository.findAll())
                .thenReturn(List.of(user));

        when(lhNoticeRepository.findAll()).thenReturn(Collections.emptyList());
        when(shAnnouncementRepository.findAll()).thenReturn(Collections.emptyList());
        when(loanOptionRepository.findAll()).thenReturn(Collections.emptyList());
        when(youthPolicyRepository.findAll()).thenReturn(Collections.emptyList());

        // 메일 전송 시 예외 발생
        doThrow(new RuntimeException("SMTP error"))
                .when(emailSender)
                .sendHtml(anyString(), anyString(), anyString());

        when(notificationsRepository.save(any(Notifications.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        notificationsService.sendDailyDigest();

        // then
        ArgumentCaptor<Notifications> captor = ArgumentCaptor.forClass(Notifications.class);
        verify(notificationsRepository, times(1)).save(captor.capture());

        Notifications saved = captor.getValue();
        assertEquals("EMAIL", saved.getType());
        assertEquals("FAILED", saved.getStatus());
        assertEquals(user, saved.getUser());
    }
}
