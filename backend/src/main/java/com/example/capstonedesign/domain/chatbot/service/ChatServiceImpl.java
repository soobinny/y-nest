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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final DbIntentDetector dbIntentDetector;
    private final SimpleTextExtractor textExtractor;
    private final DbChatSearchService dbChatSearchService;

    @Override
    @Transactional
    public ChatResponseDto chat(ChatRequestDto requestDto) {
        String userMessage = requestDto.getMessage().trim();

        // 1) 사용자 메시지 로그 저장
        ChatMessage userChat = ChatMessage.builder()
                .sender(ChatSender.USER)
                .content(userMessage)
                .build();
        chatMessageRepository.save(userChat);

        // 2) Intent 판별
        IntentType intent = dbIntentDetector.detectIntent(userMessage);
        log.info("Detected intent: {}", intent);

        // 3) Intent별 처리
        String replyText = switch (intent) {
            case HOUSING -> handleHousingQuery(userMessage);
            case FINANCE -> handleFinanceQuery(userMessage);
            case POLICY  -> handlePolicyQuery(userMessage);
            case HELP    -> buildHelpMessage();
            case UNKNOWN -> buildUnknownMessage();
        };

        // 4) 봇 메시지 로그 저장
        ChatMessage botChat = ChatMessage.builder()
                .sender(ChatSender.BOT)
                .content(replyText)
                .build();
        chatMessageRepository.save(botChat);

        // 5) 응답 반환
        return ChatResponseDto.builder()
                .reply(replyText)
                .build();
    }

    // ====== Intent별 로직 ======

    private String handleHousingQuery(String userMessage) {
        String region = textExtractor.extractRegion(userMessage);          // 서울/경기/...
        String keyword = textExtractor.extractHousingKeyword(userMessage); // 전세/월세/... 또는 ""

        String lower = userMessage.toLowerCase();
        boolean preferLh = lower.contains("lh") || userMessage.contains("엘에이치") ;
        boolean preferSh = lower.contains("sh") || userMessage.contains("에스에이치");

        List<LhNotice> lhList = dbChatSearchService
                .findTopLhByRegionAndKeyword(region, keyword, 5);
        List<ShAnnouncement> shList = dbChatSearchService
                .findTopShByRegionAndKeyword(region, keyword, 5);

        // 1) LH 우선 요청인 경우
        if (preferLh) {
            if (!lhList.isEmpty()) {
                return buildHousingReply(region, keyword, lhList, List.of());
            } else if (!shList.isEmpty()) {
                return "요청하신 LH 공고는 현재 검색되지 않았어요. 🥺\n" +
                        "대신 비슷한 SH 공고를 몇 가지 보여 드릴게요.\n\n"
                        + buildHousingReply(region, keyword, List.of(), shList);
            } else {
                return buildHousingEmptyReply(region, keyword);
            }
        }

        // 2) SH 우선 요청인 경우
        if (preferSh) {
            if (!shList.isEmpty()) {
                return buildHousingReply(region, keyword, List.of(), shList);
            } else if (!lhList.isEmpty()) {
                return "요청하신 SH 공고는 현재 검색되지 않았어요. 🥺\n" +
                        "대신 비슷한 LH 공고를 몇 가지 보여 드릴게요.\n\n"
                        + buildHousingReply(region, keyword, lhList, List.of());
            } else {
                return buildHousingEmptyReply(region, keyword);
            }
        }

        // 3) 기관 언급이 없으면 기존처럼 둘 다 섞어서
        if (lhList.isEmpty() && shList.isEmpty()) {
            return buildHousingEmptyReply(region, keyword);
        }

        return buildHousingReply(region, keyword, lhList, shList);
    }

    private String buildHousingReply(
            String region,
            String keyword,
            List<LhNotice> lhList,
            List<ShAnnouncement> shList
    ) {
        String displayRegion = (region == null || region.isBlank()) ? "전체" : region;
        boolean hasKeyword = !(keyword == null || keyword.isBlank());

        StringBuilder sb = new StringBuilder();
        if (hasKeyword) {
            sb.append(String.format(
                    "%s 지역에서 '%s' 관련 주거 공고를 몇 가지 찾아봤어요. 😆\n\n",
                    displayRegion, keyword
            ));
        } else {
            // 키워드 없으면: 지역 전체 공고 안내
            sb.append(String.format(
                    "%s 지역 주거 공고를 몇 가지 가져와 봤어요. 😆\n\n",
                    displayRegion
            ));
        }

        lhList.forEach(n -> sb.append("- [LH] ")
                .append(n.getPanNm())
                .append(" (게시: ").append(n.getPanNtStDt()).append(")\n"));

        shList.forEach(n -> sb.append("- [SH] ")
                .append(n.getTitle())
                .append(" (게시: ").append(n.getPostDate()).append(")\n"));

        sb.append("\n자세한 내용은 주거 페이지에서 해당 공고 카드를 눌러 확인해 주세요!");

        return sb.toString();
    }

    private String buildHousingEmptyReply(String region, String keyword) {
        String displayRegion = (region == null || region.isBlank()) ? "전체" : region;
        String displayKeyword = (keyword == null || keyword.isBlank()) ? "전체" : keyword;

        return String.format(
                "%s 지역에서 '%s' 관련 주거 공고를 찾지 못했어요. 😢\n" +
                        "지역이나 키워드를 조금 더 넓게 바꿔서 다시 물어봐 주세요!",
                displayRegion, displayKeyword
        );
    }


    private String handleFinanceQuery(String userMessage) {
        String keyword = textExtractor.extractFinanceKeyword(userMessage);
        List<Products> list = dbChatSearchService
                .findTopFinanceByKeyword(keyword, 5);

        if (list.isEmpty()) {
            return String.format(
                    "'%s' 관련 금융 상품을 찾지 못했어요. 😢\n" +
                            "조금 더 일반적인 키워드(예: 청년, 적금, 예금, 대출)로 다시 물어봐 주세요!",
                    keyword.isBlank() ? "전체" : keyword
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "'%s' 관련 금융 상품 몇 가지를 가져와 봤어요. 😆\n\n",
                keyword.isBlank() ? "전체" : keyword
        ));

        list.forEach(p -> sb.append("- ")
                .append(p.getName())
                .append(" / ").append(p.getProvider())
                .append("\n"));

        sb.append("\n금융 > 상품 페이지에서 각 상품을 눌러 금리/조건을 자세히 확인해 주세요!");

        return sb.toString();
    }

    private String handlePolicyQuery(String userMessage) {
        // 1) 정책용 키워드 추출
        String keyword = textExtractor.extractPolicyKeyword(userMessage);

        // 2) 1차 검색
        List<YouthPolicy> list = dbChatSearchService
                .findTopPolicyByKeyword(keyword, 5);

        // 3) 1차 검색 실패 시, 기본 키워드로 한 번 더 (fallback)
        if (list.isEmpty() && !"청년".equals(keyword)) {
            list = dbChatSearchService.findTopPolicyByKeyword("청년", 5);
        }

        if (list.isEmpty()) {
            return "청년 정책 검색 결과가 없어요. 😢\n" +
                    "조금 더 짧은 키워드(예: 전세, 월세, 취업, 창업, 교통 등)로 다시 물어봐 주세요!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("청년 정책 관련해서 이런 것들을 찾아봤어요. 😆\n\n");

        list.forEach(p -> sb.append("- ")
                .append(p.getPolicyName())
                .append(" / ").append(p.getDescription())
                .append("\n"));

        sb.append("\n정책 페이지에서 관심 있는 정책을 눌러 상세 내용을 확인해 주세요!");

        return sb.toString();
    }

    private String buildHelpMessage() {
        return """
                저는 Y-Nest 챗봇, 네스티예요. 🪽
                
                - "서울 전세 지원 뭐 있어?" 처럼 지역 + 전세/월세 키워드로 물어보시면 주거 공고를 찾아 드려요.
                - "청년 적금 추천해 줘" 처럼 금융 상품 키워드를 질문하시면 관련 상품을 보여 드려요.
                - "청년 정책 알려 줘" 처럼 정책 관련 키워드를 말해 주시면 관련 정책을 찾아드려요.

                화면의 주거/금융/정책 탭과 함께 사용하면 더 편하게 혜택을 찾을 수 있어요!
                """;
    }

    private String buildUnknownMessage() {
        return """
                아직 제가 이해하기 어려운 질문이에요. 🥺
                - 주거(전세/월세/청년주택)
                - 금융(예금/적금/대출)
                - 청년 정책(지원금/사업/보조금)
                관련해서 다시 한번 물어봐 주시면, 가능한 범위에서 찾아볼게요!
                """;
    }
}
