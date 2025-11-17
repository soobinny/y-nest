package com.example.capstonedesign.domain.chatbot.service.DB;

import com.example.capstonedesign.domain.chatbot.entity.IntentType;
import org.springframework.stereotype.Component;

@Component
public class DbIntentDetector {

    public IntentType detectIntent(String message) {
        if (message == null || message.isBlank()) {
            return IntentType.UNKNOWN;
        }

        String text = message.toLowerCase();

        // 0단계: 기관 코드(LH/SH)만 입력해도 주거로 인식
        // 원본 문자열에서 대문자 LH/SH를 우선 체크 (cash 같은 오탐 방지)
        if (message.contains("LH") || message.contains("엘에이치")
                || message.contains("SH") || message.contains("에스에이치")) {
            return IntentType.HOUSING;
        }

        // 1단계: 지역 이름만 있어도 HOUSING으로 인식
        if (containsRegionKeyword(message)) {
            return IntentType.HOUSING;
        }

        // 주거 관련 키워드
        if (text.contains("전세") || text.contains("월세")
                || text.contains("임대") || text.contains("공고")
                || text.contains("주택") || text.contains("청년주택")) {
            return IntentType.HOUSING;
        }

        // 금융 관련 키워드
        if (text.contains("대출") || text.contains("예금")
                || text.contains("적금") || text.contains("금리")
                || text.contains("통장")) {
            return IntentType.FINANCE;
        }

        // 정책 관련 키워드
        if (text.contains("정책") || text.contains("청년정책") || text.contains("지원금")
                || text.contains("사업") || text.contains("보조금")) {
            return IntentType.POLICY;
        }

        // 도움/사용법
        if (text.contains("사용법") || text.contains("어떻게")
                || text.contains("도움말") || text.contains("설명")
                || text.contains("뭐 하는 서비스")) {
            return IntentType.HELP;
        }

        return IntentType.UNKNOWN;
    }

    private boolean containsRegionKeyword(String message) {
        if (message == null) return false;

        String[] regions = {
                "서울", "경기", "인천", "부산", "대구",
                "대전", "광주", "울산", "세종",
                "강원", "충북", "충남", "전북", "전남",
                "경북", "경남", "제주"
        };
        for (String r : regions) {
            if (message.contains(r)) return true;
        }
        return false;
    }
}
