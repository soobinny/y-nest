package com.example.capstonedesign.domain.users.config;

import com.example.capstonedesign.domain.users.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

/**
 * JwtTokenProvider
 * -------------------------------------------------
 * - JWT 토큰의 생성 및 검증을 담당하는 유틸리티 클래스
 * - HS256(HMAC SHA-256) 알고리즘을 이용해 서명
 * - email(subject), userId, role 정보를 포함한 토큰 발급
 * - 유효 기간(expiration) 검증 및 서명키 검증 수행
 */
@Component
public class JwtTokenProvider {

    /** JWT 서명을 위한 비밀 키 */
    private final Key key;

    /** 토큰 만료 시간 (초 단위, 기본 3600초 = 1시간) */
    @Getter
    private final long expirySeconds;

    /**
     * JwtTokenProvider 생성자
     * -------------------------------------------------
     * - 환경 변수 또는 설정 파일(application.yml / .properties)에서
     *   JWT 서명용 비밀키와 만료 시간을 주입받음
     *
     * @param secret        JWT 서명용 비밀키 (Base64 또는 일반 문자열)
     * @param expirySeconds 토큰 유효 시간 (초 단위)
     */
    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expires-seconds:3600}") long expirySeconds) {
        // 1. 비밀키 문자열을 바이트 배열로 변환하여 HMAC-SHA256 키 생성
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        // 2. 토큰 유효 시간 설정
        this.expirySeconds = expirySeconds;
    }

    /**
     * generate
     * -------------------------------------------------
     * - 사용자 정보를 기반으로 JWT를 생성
     * - userId, role을 claim에 추가하고 HS256 알고리즘으로 서명
     *
     * @param userId 사용자 ID (claim: "userId")
     * @param email  사용자 이메일 (JWT subject)
     * @param role   사용자 권한 (claim: "role")
     * @return 생성된 JWT 문자열
     */
    public String generate(Long userId, String email, UserRole role) {
        // 1. 현재 시각 및 만료 시각 계산
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirySeconds);

        // 2. JWT 빌더로 토큰 생성
        return Jwts.builder()
                .setSubject(email)                     // JWT subject 설정 (이메일)
                .claim("userId", userId)            // 사용자 ID claim
                .claim("role", role.name())         // 권한 claim
                .setIssuedAt(Date.from(now))           // 발급 시간(iat)
                .setExpiration(Date.from(exp))         // 만료 시간(exp)
                .signWith(key, SignatureAlgorithm.HS256) // HS256 알고리즘으로 서명
                .compact();                            // 최종 토큰 문자열 생성
    }

    /**
     * parse
     * -------------------------------------------------
     * - JWT 문자열을 파싱하여 Claims 반환
     * - 서명 유효성 및 만료 여부를 검증
     *
     * @param token 클라이언트로부터 전달받은 JWT 문자열
     * @return 파싱된 JWT Claims 객체 (userId, role 등 포함)
     * @throws io.jsonwebtoken.JwtException 유효하지 않거나 만료된 토큰일 경우 예외 발생
     */
    public Jws<Claims> parse(String token) {
        // 1. 서명 키를 설정하여 파서 생성
        // 2. 토큰 파싱 및 서명 검증 수행
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}
