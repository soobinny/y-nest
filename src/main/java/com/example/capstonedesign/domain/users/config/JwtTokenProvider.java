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
 * - JWT 토큰 생성 및 검증을 담당하는 유틸리티 클래스
 * - HS256(HMAC SHA-256) 알고리즘 기반 서명 사용
 * - email(subject) + role(claim) 기반 토큰 발급
 */
@Component
public class JwtTokenProvider {

    /** 토큰 서명용 비밀 키 */
    private final Key key;

    /** 토큰 만료 시간 (초 단위) */
    @Getter
    private final long expirySeconds;

    /**
     * JwtTokenProvider 생성자
     *
     * @param secret        application.properties 또는 환경 변수에 정의된 jwt.secret
     * @param expirySeconds 토큰 유효 시간 (기본값: 3600초 = 1시간)
     */
    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expires-seconds:3600}") long expirySeconds) {
        // 비밀키 문자열을 기반으로 HMAC-SHA 키 생성
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirySeconds = expirySeconds;
    }

    /**
     * JWT 토큰 생성
     *
     * @param email 사용자 이메일 (subject)
     * @param role  사용자 권한 (claim: "role")
     * @return JWT 문자열
     */
    public String generate(String email, UserRole role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirySeconds);

        return Jwts.builder()
                .setSubject(email)                // 사용자 식별자
                .claim("role", role.name())    // 권한 정보 claim 추가
                .setIssuedAt(Date.from(now))      // 발급 시간
                .setExpiration(Date.from(exp))    // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // HMAC-SHA256 서명
                .compact();
    }

    /**
     * JWT 토큰 파싱 및 검증
     *
     * @param token 클라이언트가 전달한 JWT
     * @return 파싱된 Claims (subject, role 등)
     * @throws io.jsonwebtoken.JwtException (유효하지 않거나 만료된 경우)
     */
    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)   // 서명 키 설정
                .build()
                .parseClaimsJws(token);
    }
}
