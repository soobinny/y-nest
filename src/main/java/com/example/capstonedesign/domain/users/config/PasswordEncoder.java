package com.example.capstonedesign.domain.users.config;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * PasswordEncoder
 * -------------------------------------------------
 * - 사용자 비밀번호를 안전하게 해싱하고 검증하는 유틸 클래스
 * - BCrypt 알고리즘을 사용하여 단방향 암호화 처리
 * - 동일한 입력 비밀번호라도 해시 결과가 매번 달라져 보안성 강화
 */
@Component
public class PasswordEncoder {

    /**
     * 비밀번호 해싱
     *
     * @param rawPassword 원본 비밀번호 (평문)
     * @return 해싱된 비밀번호 문자열
     */
    public String encode(String rawPassword) {
        // BCrypt.MIN_COST → 가장 낮은 비용 인자(빠르지만 보안성 낮음)
        // 실서비스에서는 10~12 정도로 설정 권장
        return BCrypt.withDefaults().hashToString(BCrypt.MIN_COST, rawPassword.toCharArray());
    }

    /**
     * 비밀번호 검증
     *
     * @param rawPassword     입력된 원본 비밀번호
     * @param encodedPassword 저장된 해싱된 비밀번호
     * @return 일치 여부 (true = 비밀번호 일치)
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        BCrypt.Result result = BCrypt.verifyer().verify(rawPassword.toCharArray(), encodedPassword);
        return result.verified;
    }
}
