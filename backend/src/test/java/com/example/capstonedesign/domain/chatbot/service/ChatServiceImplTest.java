package com.example.capstonedesign.domain.chatbot.service;

import com.example.capstonedesign.domain.chatbot.dto.request.ChatRequestDto;
import com.example.capstonedesign.domain.chatbot.dto.response.ChatResponseDto;
import com.example.capstonedesign.domain.chatbot.entity.ChatMessage;
import com.example.capstonedesign.domain.chatbot.entity.ChatSender;
import com.example.capstonedesign.domain.chatbot.entity.IntentType;
import com.example.capstonedesign.domain.chatbot.repository.ChatMessageRepository;
import com.example.capstonedesign.domain.chatbot.service.DB.DbChatSearchService;
import com.example.capstonedesign.domain.chatbot.service.DB.DbIntentDetector;
import com.example.capstonedesign.domain.housingannouncements.entity.LhNotice;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.shannouncements.entity.ShAnnouncement;
import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private DbIntentDetector dbIntentDetector;

    @Mock
    private SimpleTextExtractor textExtractor;

    @Mock
    private DbChatSearchService dbChatSearchService;

    @InjectMocks
    private ChatServiceImpl chatService;

    private ChatRequestDto createRequest(String message) {
        ChatRequestDto dto = new ChatRequestDto();
        dto.setMessage(message);
        return dto;
    }

    @Test
    void chat_housingIntent_returnsHousingReply_andSaveMessages() {
        // given
        String message = "서울 전세 지원 뭐 있어?";
        ChatRequestDto requestDto = createRequest(message);

        when(dbIntentDetector.detectIntent(anyString()))
                .thenReturn(IntentType.HOUSING);

        when(textExtractor.extractRegion(message)).thenReturn("서울");
        when(textExtractor.extractHousingKeyword(message)).thenReturn("전세");

        // LhNotice / ShAnnouncement는 실제 엔티티 대신 mock으로 최소 필드만 사용
        LhNotice lhNotice = mock(LhNotice.class);
        when(lhNotice.getPanNm()).thenReturn("서울 청년전세 임대주택");
        when(lhNotice.getPanNtStDt()).thenReturn("2025-01-01");
        ShAnnouncement shAnnouncement = mock(ShAnnouncement.class);
        when(shAnnouncement.getTitle()).thenReturn("서울 청년 월세 지원");
        when(shAnnouncement.getPostDate()).thenReturn(LocalDate.of(2025, 1, 2));

        when(dbChatSearchService.findTopLhByRegionAndKeyword("서울", "전세", 5))
                .thenReturn(List.of(lhNotice));
        when(dbChatSearchService.findTopShByRegionAndKeyword("서울", "전세", 5))
                .thenReturn(List.of(shAnnouncement));

        // when
        ChatResponseDto response = chatService.chat(requestDto);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getReply())
                .contains("서울 지역에서 '전세' 관련 주거 공고를 몇 가지 찾아봤어요")
                .contains("[LH]")
                .contains("[SH]");

        // 사용자/봇 메시지 각각 한 번씩 저장되는지 확인
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(captor.capture());

        List<ChatMessage> savedMessages = captor.getAllValues();
        assertThat(savedMessages)
                .extracting(ChatMessage::getSender)
                .containsExactly(ChatSender.USER, ChatSender.BOT);

        assertThat(savedMessages.get(0).getContent()).isEqualTo(message);
    }

    @Test
    void chat_financeIntent_returnsFinanceReply() {
        // given
        String message = "청년 적금 추천해 줘";
        ChatRequestDto requestDto = createRequest(message);

        when(dbIntentDetector.detectIntent(anyString()))
                .thenReturn(IntentType.FINANCE);

        when(textExtractor.extractFinanceKeyword(message)).thenReturn("청년 적금");

        Products p1 = mock(Products.class);
        when(p1.getName()).thenReturn("청년 희망 적금");
        when(p1.getProvider()).thenReturn("OO은행");

        when(dbChatSearchService.findTopFinanceByKeyword("청년 적금", 5))
                .thenReturn(List.of(p1));

        // when
        ChatResponseDto response = chatService.chat(requestDto);

        // then
        assertThat(response.getReply())
                .contains("관련 금융 상품 몇 가지를 가져와 봤어요")
                .contains("청년 희망 적금")
                .contains("OO은행");
    }

    @Test
    void chat_policyIntent_fallbackToDefaultKeywordWhenEmpty() {
        // given
        String message = "교통비 지원 정책 있어?";
        ChatRequestDto requestDto = createRequest(message);

        when(dbIntentDetector.detectIntent(anyString()))
                .thenReturn(IntentType.POLICY);

        when(textExtractor.extractPolicyKeyword(message)).thenReturn("교통비");

        // 1차 검색은 빈 리스트
        when(dbChatSearchService.findTopPolicyByKeyword("교통비", 5))
                .thenReturn(List.of());

        // '청년' 기본 키워드로 fallback
        YouthPolicy policy = mock(YouthPolicy.class);
        when(policy.getPolicyName()).thenReturn("청년 교통비 지원");
        when(policy.getDescription()).thenReturn("청년 대상 대중교통비 지원 사업");

        when(dbChatSearchService.findTopPolicyByKeyword("청년", 5))
                .thenReturn(List.of(policy));

        // when
        ChatResponseDto response = chatService.chat(requestDto);

        // then
        assertThat(response.getReply())
                .contains("청년 정책 관련해서 이런 것들을 찾아봤어요")
                .contains("청년 교통비 지원")
                .contains("청년 대상 대중교통비 지원 사업");
    }

    @Test
    void chat_helpIntent_returnsHelpMessage() {
        // given
        String message = "어떻게 물어봐야 해?";
        ChatRequestDto requestDto = createRequest(message);

        when(dbIntentDetector.detectIntent(anyString()))
                .thenReturn(IntentType.HELP);

        // when
        ChatResponseDto response = chatService.chat(requestDto);

        // then
        assertThat(response.getReply())
                .contains("저는 Y-Nest 챗봇, 네스티예요");
    }

    @Test
    void chat_unknownIntent_returnsUnknownMessage() {
        // given
        String message = "의미 없는 문장 아무거나...";
        ChatRequestDto requestDto = createRequest(message);

        when(dbIntentDetector.detectIntent(anyString()))
                .thenReturn(IntentType.UNKNOWN);

        // when
        ChatResponseDto response = chatService.chat(requestDto);

        // then
        assertThat(response.getReply())
                .contains("아직 제가 이해하기 어려운 질문이에요");
    }

    @Test
    void chat_housingIntent_preferLh_onlyShExists_returnsShFallbackMessage() {
        // given
        String message = "LH 서울 전세 지원 보고 싶어";  // 'lh' 포함 → preferLh = true
        ChatRequestDto requestDto = createRequest(message);

        when(dbIntentDetector.detectIntent(anyString()))
                .thenReturn(IntentType.HOUSING);

        when(textExtractor.extractRegion(message)).thenReturn("서울");
        when(textExtractor.extractHousingKeyword(message)).thenReturn("전세");

        // LH는 없음, SH만 존재
        when(dbChatSearchService.findTopLhByRegionAndKeyword("서울", "전세", 5))
                .thenReturn(List.of());
        ShAnnouncement shAnnouncement = mock(ShAnnouncement.class);
        when(shAnnouncement.getTitle()).thenReturn("서울 청년 월세 지원");
        when(dbChatSearchService.findTopShByRegionAndKeyword("서울", "전세", 5))
                .thenReturn(List.of(shAnnouncement));

        // when
        ChatResponseDto response = chatService.chat(requestDto);

        // then
        assertThat(response.getReply())
                .contains("요청하신 LH 공고는 현재 검색되지 않았어요")
                .contains("대신 비슷한 SH 공고를 몇 가지 보여 드릴게요")
                .contains("[SH]");
    }

    @Test
    void chat_housingIntent_preferLh_noResults_returnsEmptyReply() {
        // given
        String message = "LH 부산 월세 지원 알려줘";  // preferLh = true
        ChatRequestDto requestDto = createRequest(message);

        when(dbIntentDetector.detectIntent(anyString()))
                .thenReturn(IntentType.HOUSING);

        when(textExtractor.extractRegion(message)).thenReturn("부산");
        when(textExtractor.extractHousingKeyword(message)).thenReturn("월세");

        // LH/SH 모두 없음
        when(dbChatSearchService.findTopLhByRegionAndKeyword("부산", "월세", 5))
                .thenReturn(List.of());
        when(dbChatSearchService.findTopShByRegionAndKeyword("부산", "월세", 5))
                .thenReturn(List.of());

        // when
        ChatResponseDto response = chatService.chat(requestDto);

        // then
        assertThat(response.getReply())
                .contains("부산 지역에서 '월세' 관련 주거 공고를 찾지 못했어요")
                .contains("지역이나 키워드를 조금 더 넓게 바꿔서 다시 물어봐 주세요");
    }

    @Test
    void chat_housingIntent_preferSh_onlyLhExists_returnsLhFallbackMessage() {
        // given
        String message = "SH 경기 전세 지원 알려 줘";  // 'sh' 포함 → preferSh = true
        ChatRequestDto requestDto = createRequest(message);

        when(dbIntentDetector.detectIntent(anyString()))
                .thenReturn(IntentType.HOUSING);

        when(textExtractor.extractRegion(message)).thenReturn("경기");
        when(textExtractor.extractHousingKeyword(message)).thenReturn("전세");

        // SH는 없음, LH만 존재
        when(dbChatSearchService.findTopShByRegionAndKeyword("경기", "전세", 5))
                .thenReturn(List.of());
        LhNotice lhNotice = mock(LhNotice.class);
        when(lhNotice.getPanNm()).thenReturn("경기 청년 전세 임대주택");
        when(dbChatSearchService.findTopLhByRegionAndKeyword("경기", "전세", 5))
                .thenReturn(List.of(lhNotice));

        // when
        ChatResponseDto response = chatService.chat(requestDto);

        // then
        assertThat(response.getReply())
                .contains("요청하신 SH 공고는 현재 검색되지 않았어요")
                .contains("대신 비슷한 LH 공고를 몇 가지 보여 드릴게요")
                .contains("[LH]");
    }

    @Test
    void chat_housingIntent_noAgencyMention_andNoResults_usesDefaultRegionAndKeyword() {
        // given
        String message = "집 관련 지원 뭐 있어?";  // LH/SH 언급 없음 → preferLh/ preferSh 둘 다 false
        ChatRequestDto requestDto = createRequest(message);

        when(dbIntentDetector.detectIntent(anyString()))
                .thenReturn(IntentType.HOUSING);

        // region/keyword를 null/blank로 내려 보낼 때 처리 확인
        when(textExtractor.extractRegion(message)).thenReturn(null);
        when(textExtractor.extractHousingKeyword(message)).thenReturn("");

        // LH/SH 모두 없음
        when(dbChatSearchService.findTopLhByRegionAndKeyword(null, "", 5))
                .thenReturn(List.of());
        when(dbChatSearchService.findTopShByRegionAndKeyword(null, "", 5))
                .thenReturn(List.of());

        // when
        ChatResponseDto response = chatService.chat(requestDto);

        // then
        assertThat(response.getReply())
                .contains("전체 지역에서 '전체' 관련 주거 공고를 찾지 못했어요")
                .contains("지역이나 키워드를 조금 더 넓게 바꿔서 다시 물어봐 주세요");
    }
}
