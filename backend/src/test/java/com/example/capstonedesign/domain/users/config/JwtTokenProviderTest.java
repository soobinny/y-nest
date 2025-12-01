package com.example.capstonedesign.domain.users.config;

import com.example.capstonedesign.domain.users.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JwtTokenProvider 단위 테스트
 * ---------------------------------------------
 * - generate()로 생성한 토큰을 parse()로 다시 파싱했을 때
 *   subject, userId, role, exp 등이 예상대로 담겨 있는지 검증
 * - 잘못된 토큰을 파싱할 경우 예외가 발생하는지 검증
 */
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "01234567890123456789012345678901"; // 32바이트 이상

    @Test
    @DisplayName("generate() - userId, email, role을 포함한 JWT를 생성하고 parse()로 복원할 수 있다")
    void generateAndParse_validToken_roundTripSuccess() {
        // given
        long expirySeconds = 3600L; // 1시간
        JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, expirySeconds);

        Long userId = 1L;
        String email = "test@example.com";
        UserRole role = UserRole.USER; // 프로젝트 enum 값에 맞게 필요 시 수정

        // when
        Instant before = Instant.now();
        String token = provider.generate(userId, email, role);
        Instant after = Instant.now();

        Jws<Claims> jws = provider.parse(token);
        Claims claims = jws.getBody();

        // then
        assertThat(token).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo(email);
        assertThat(claims.get("userId", Long.class)).isEqualTo(userId);
        assertThat(claims.get("role", String.class)).isEqualTo(role.name());

        // 시간 관련 검증
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        // iat는 before~after 사이여야 함 (여유로 1초 마진)
        assertThat(issuedAt.toInstant())
                .isAfterOrEqualTo(before.minus(1, ChronoUnit.SECONDS))
                .isBeforeOrEqualTo(after.plus(1, ChronoUnit.SECONDS));

        // exp는 iat + expirySeconds 근처여야 함 (±5초 허용)
        Instant expectedExpMin = issuedAt.toInstant().plus(expirySeconds - 5, ChronoUnit.SECONDS);
        Instant expectedExpMax = issuedAt.toInstant().plus(expirySeconds + 5, ChronoUnit.SECONDS);

        assertThat(expiration.toInstant())
                .isAfter(expectedExpMin)
                .isBefore(expectedExpMax);
    }

    @Test
    @DisplayName("expirySeconds 게터는 생성자에서 설정한 값을 반환한다")
    void getExpirySeconds_returnsConstructorValue() {
        // given
        long expirySeconds = 7200L; // 2시간
        JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, expirySeconds);

        // when & then
        assertThat(provider.getExpirySeconds()).isEqualTo(expirySeconds);
    }

    @Test
    @DisplayName("parse() - 형식이 잘못된 토큰을 파싱하면 JwtException이 발생한다")
    void parse_invalidToken_throwsJwtException() {
        // given
        JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, 3600L);
        String invalidToken = "this.is.not.valid.jwt";

        // when & then
        assertThrows(JwtException.class, () -> provider.parse(invalidToken));
    }
}
