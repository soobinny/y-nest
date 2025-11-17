package com.example.capstonedesign.domain.chatbot.service;

import org.springframework.stereotype.Component;

@Component
public class SimpleTextExtractor {

    /**
     * 질문 안에서 지역 이름(서울/경기/인천/부산...)을 간단히 찾아냄
     * 못 찾으면 "전체" 반환
     */
    public String extractRegion(String message) {
        if (message == null) return "전체";

        if (message.contains("서울")) return "서울";
        if (message.contains("경기")) return "경기";
        if (message.contains("인천")) return "인천";
        if (message.contains("부산")) return "부산";
        if (message.contains("대구")) return "대구";
        if (message.contains("대전")) return "대전";
        if (message.contains("광주")) return "광주";
        if (message.contains("울산")) return "울산";

        return "전체";
    }

    /**
     * 주거 관련 키워드를 간단히 뽑는 예시
     */
    public String extractHousingKeyword(String message) {
        if (message == null) return "";

        if (message.contains("전세")) return "전세";
        if (message.contains("월세")) return "월세";
        if (message.contains("청년")) return "청년";
        if (message.contains("임대")) return "임대";

        return "";
    }

    /**
     * 금융 관련 키워드 예시
     */
    public String extractFinanceKeyword(String message) {
        if (message == null) return "";

        if (message.contains("대출")) return "대출";
        if (message.contains("적금")) return "적금";
        if (message.contains("예금")) return "예금";
        if (message.contains("청년")) return "청년";

        return "";
    }

    public String extractPolicyKeyword(String message) {
        if (message == null || message.isBlank()) return "";

        String text = message.toLowerCase();

        // 1순위: 구체 키워드
        if (text.contains("취업")) return "취업";
        if (text.contains("창업")) return "창업";
        if (text.contains("교통")) return "교통";
        if (text.contains("전세") || text.contains("월세") || text.contains("주거")) return "전세";

        // 2순위: "청년 정책" / "청년정책"
        if (text.contains("청년 정책") || text.contains("청년정책")) return "청년";

        // 3순위: 그냥 "청년"이라도
        if (text.contains("청년")) return "청년";

        // 4순위: 최소한 "정책"이라도
        if (text.contains("정책")) return "정책";

        return "";
    }
}
