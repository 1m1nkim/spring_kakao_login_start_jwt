package com.ll.b20250227.controller;

import com.ll.b20250227.jwt.JwtProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final JwtProvider jwtProvider;

    public AuthController(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    // Refresh Token 재발급 API
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Refresh-Token") String refreshToken) {
        // Refresh Token 검증
        if (refreshToken == null || !jwtProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 Refresh Token입니다.");
        }
        String tokenType = jwtProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token이 아닙니다.");
        }
        // 토큰의 subject(사용자 식별자)를 이용해 새로운 토큰 발급
        String subject = jwtProvider.getSubject(refreshToken);
        String newAccessToken = jwtProvider.createAccessToken(subject);
        String newRefreshToken = jwtProvider.createRefreshToken(subject);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);

        return ResponseEntity.ok(tokens);
    }
}
