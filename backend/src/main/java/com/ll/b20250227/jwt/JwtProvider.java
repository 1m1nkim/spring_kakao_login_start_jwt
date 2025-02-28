package com.ll.b20250227.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

    // 비밀키: 운영환경에서는 외부 설정으로 관리하세요.
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // 토큰 유효시간 (예: 1시간)
    private final long validityInMilliseconds = 5000; // 10분
    private final long refreshTokenValidity = 7 * 24 * 60 * 60 * 1000L; // 7일

    // 토큰 생성: 주로 사용자 식별값(username 혹은 userId)을 subject로 설정
    public String createAccessToken(String subject) {
        Claims claims = Jwts.claims().setSubject(subject);
        claims.put("type", "access");
        // 추가 클레임 설정 가능

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String subject) {
        Claims claims = Jwts.claims().setSubject(subject);
        claims.put("type", "refresh");

        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidity);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰이 유효하지 않음
            return false;
        }
    }

    // 토큰에서 subject(예, username 또는 userId) 추출
    public String getSubject(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String getTokenType(String token) {
        return (String) Jwts.parserBuilder().setSigningKey(key).build().
                parseClaimsJws(token).getBody().getSubject();
    }
}
