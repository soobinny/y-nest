package com.example.capstonedesign.domain.chatbot.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleTextExtractorTest {

    private final SimpleTextExtractor extractor = new SimpleTextExtractor();

    @Test
    void extractRegion_returnsCityName_whenMessageContainsRegion() {
        assertThat(extractor.extractRegion("서울 전세 지원 뭐 있어?"))
                .isEqualTo("서울");
        assertThat(extractor.extractRegion("경기 청년 월세"))
                .isEqualTo("경기");
        assertThat(extractor.extractRegion("부산에 청년 주택 있어?"))
                .isEqualTo("부산");
    }

    @Test
    void extractRegion_returns전체_whenMessageIsNullOrNoRegion() {
        assertThat(extractor.extractRegion(null))
                .isEqualTo("전체");
        assertThat(extractor.extractRegion("집 관련 지원 뭐 있어?"))
                .isEqualTo("전체");
    }

    @Test
    void extractHousingKeyword_returnsMatchingKeywordOrEmpty() {
        assertThat(extractor.extractHousingKeyword("서울 전세 지원 뭐 있어?"))
                .isEqualTo("전세");
        assertThat(extractor.extractHousingKeyword("부산 월세 청년 주택 알려줘"))
                .isEqualTo("월세");
        assertThat(extractor.extractHousingKeyword("청년 임대 주택 있어?"))
                .isEqualTo("청년");
        assertThat(extractor.extractHousingKeyword("임대 아파트 궁금해"))
                .isEqualTo("임대");
        assertThat(extractor.extractHousingKeyword("집 관련 지원 뭐 있어?"))
                .isEqualTo("");
        assertThat(extractor.extractHousingKeyword(null))
                .isEqualTo("");
    }

    @Test
    void extractFinanceKeyword_returnsMatchingKeywordOrEmpty() {
        assertThat(extractor.extractFinanceKeyword("청년 대출 상품 뭐 있어?"))
                .isEqualTo("대출");
        assertThat(extractor.extractFinanceKeyword("청년 적금 추천해 줘"))
                .isEqualTo("적금");
        assertThat(extractor.extractFinanceKeyword("예금 금리 좋은 거 알려줘"))
                .isEqualTo("예금");
        assertThat(extractor.extractFinanceKeyword("청년 통장 관련 지원 뭐 있어?"))
                .isEqualTo("청년");
        assertThat(extractor.extractFinanceKeyword("돈 관련 도움 없나?"))
                .isEqualTo("");
        assertThat(extractor.extractFinanceKeyword(null))
                .isEqualTo("");
    }

    @Test
    void extractPolicyKeyword_returnsSpecificKeywordsWithPriority() {
        // 1순위: 취업/창업/교통
        assertThat(extractor.extractPolicyKeyword("취업 지원 정책 알려줘"))
                .isEqualTo("취업");
        assertThat(extractor.extractPolicyKeyword("창업 관련 청년 정책 있어?"))
                .isEqualTo("창업");
        assertThat(extractor.extractPolicyKeyword("교통비 지원 정책 있어?"))
                .isEqualTo("교통");

        // 전세/월세/주거 → "전세"
        assertThat(extractor.extractPolicyKeyword("전세 지원 정책"))
                .isEqualTo("전세");
        assertThat(extractor.extractPolicyKeyword("월세 보조 정책"))
                .isEqualTo("전세");
        assertThat(extractor.extractPolicyKeyword("주거 지원 정책 궁금해"))
                .isEqualTo("전세");
    }

    @Test
    void extractPolicyKeyword_returns청년_whenContains청년정책Or청년정책붙여쓰기() {
        assertThat(extractor.extractPolicyKeyword("청년 정책 알려줘"))
                .isEqualTo("청년");
        assertThat(extractor.extractPolicyKeyword("청년정책 뭐 있어?"))
                .isEqualTo("청년");
    }

    @Test
    void extractPolicyKeyword_returns청년_whenOnly청년Exists() {
        assertThat(extractor.extractPolicyKeyword("청년 지원 뭐 있어?"))
                .isEqualTo("청년");
    }

    @Test
    void extractPolicyKeyword_returns정책_whenOnly정책Exists() {
        assertThat(extractor.extractPolicyKeyword("지원 정책 종류 알려줘"))
                .isEqualTo("정책");
    }

    @Test
    void extractPolicyKeyword_returnsEmpty_whenNoKeywordMatched() {
        assertThat(extractor.extractPolicyKeyword("그냥 궁금해서 물어봄"))
                .isEqualTo("");
        assertThat(extractor.extractPolicyKeyword(null))
                .isEqualTo("");
        assertThat(extractor.extractPolicyKeyword("   "))
                .isEqualTo("");
    }
}
