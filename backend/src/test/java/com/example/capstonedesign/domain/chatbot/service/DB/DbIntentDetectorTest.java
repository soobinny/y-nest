package com.example.capstonedesign.domain.chatbot.service.DB;

import com.example.capstonedesign.domain.chatbot.entity.IntentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DbIntentDetectorTest {

    private final DbIntentDetector detector = new DbIntentDetector();

    @Test
    void detectIntent_returnsUnknown_whenMessageIsNullOrBlank() {
        assertThat(detector.detectIntent(null))
                .isEqualTo(IntentType.UNKNOWN);
        assertThat(detector.detectIntent(""))
                .isEqualTo(IntentType.UNKNOWN);
        assertThat(detector.detectIntent("   "))
                .isEqualTo(IntentType.UNKNOWN);
    }

    @Test
    void detectIntent_returnsHousing_whenContainsLhOrShOrKoreanAlias() {
        assertThat(detector.detectIntent("LH 공고 보여 줘"))
                .isEqualTo(IntentType.HOUSING);
        assertThat(detector.detectIntent("엘에이치 전세 궁금해"))
                .isEqualTo(IntentType.HOUSING);
        assertThat(detector.detectIntent("SH 공고 알려줘"))
                .isEqualTo(IntentType.HOUSING);
        assertThat(detector.detectIntent("에스에이치 청년주택 있어?"))
                .isEqualTo(IntentType.HOUSING);
    }

    @Test
    void detectIntent_returnsHousing_whenContainsRegionNameOnly() {
        assertThat(detector.detectIntent("서울 청년 뭐 있어?"))
                .isEqualTo(IntentType.HOUSING);
        assertThat(detector.detectIntent("부산 지원 궁금해"))
                .isEqualTo(IntentType.HOUSING);
        assertThat(detector.detectIntent("경기 쪽 혜택 알려 줘"))
                .isEqualTo(IntentType.HOUSING);
    }

    @Test
    void detectIntent_returnsHousing_whenContainsHousingKeywords() {
        assertThat(detector.detectIntent("전세 지원 뭐 있어?"))
                .isEqualTo(IntentType.HOUSING);
        assertThat(detector.detectIntent("월세 보조 받아?"))
                .isEqualTo(IntentType.HOUSING);
        assertThat(detector.detectIntent("임대 주택 신청 방법 알려 줘"))
                .isEqualTo(IntentType.HOUSING);
        assertThat(detector.detectIntent("공고 어디서 볼 수 있어?"))
                .isEqualTo(IntentType.HOUSING);
        assertThat(detector.detectIntent("청년주택 정보 좀"))
                .isEqualTo(IntentType.HOUSING);
        assertThat(detector.detectIntent("주택 관련 혜택 찾아 줘"))
                .isEqualTo(IntentType.HOUSING);
    }

    @Test
    void detectIntent_returnsFinance_whenContainsFinanceKeywords() {
        assertThat(detector.detectIntent("청년 대출 금리 알려 줘"))
                .isEqualTo(IntentType.FINANCE);
        assertThat(detector.detectIntent("청년 적금 추천해 줘"))
                .isEqualTo(IntentType.FINANCE);
        assertThat(detector.detectIntent("예금 상품 비교해 줘"))
                .isEqualTo(IntentType.FINANCE);
        assertThat(detector.detectIntent("금리 높은 통장 뭐 있어?"))
                .isEqualTo(IntentType.FINANCE);
    }

    @Test
    void detectIntent_returnsPolicy_whenContainsPolicyKeywords() {
        assertThat(detector.detectIntent("청년 정책 알려줘"))
                .isEqualTo(IntentType.POLICY);
        assertThat(detector.detectIntent("청년정책 뭐 있나?"))
                .isEqualTo(IntentType.POLICY);
        assertThat(detector.detectIntent("청년 지원금 받을 수 있어?"))
                .isEqualTo(IntentType.POLICY);
        assertThat(detector.detectIntent("청년 대상 사업 지원 궁금해"))
                .isEqualTo(IntentType.POLICY);
        assertThat(detector.detectIntent("보조금 관련 정책 있어?"))
                .isEqualTo(IntentType.POLICY);
    }

    @Test
    void detectIntent_returnsHelp_whenContainsHelpKeywords() {
        assertThat(detector.detectIntent("사용법 알려 줘"))
                .isEqualTo(IntentType.HELP);
        assertThat(detector.detectIntent("이거 어떻게 쓰는 거야?"))
                .isEqualTo(IntentType.HELP);
        assertThat(detector.detectIntent("도움말 보여줘"))
                .isEqualTo(IntentType.HELP);
        assertThat(detector.detectIntent("서비스 설명 좀 해 줘"))
                .isEqualTo(IntentType.HELP);
        assertThat(detector.detectIntent("뭐 하는 서비스야?"))
                .isEqualTo(IntentType.HELP);
    }

    @Test
    void detectIntent_returnsUnknown_whenNoKeywordsMatched() {
        assertThat(detector.detectIntent("그냥 아무 말이나 해 본 거야"))
                .isEqualTo(IntentType.UNKNOWN);
        assertThat(detector.detectIntent("테스트 중입니다"))
                .isEqualTo(IntentType.UNKNOWN);
    }
}
