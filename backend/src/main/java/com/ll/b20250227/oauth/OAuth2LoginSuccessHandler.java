package com.ll.b20250227.oauth;

import com.ll.b20250227.jwt.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtProvider jwtProvider;

    public OAuth2LoginSuccessHandler(final JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long kakaoId = (Long) oAuth2User.getAttributes().get("id");
        String accessToken = jwtProvider.createAccessToken(String.valueOf(kakaoId));
        String refreshToken = jwtProvider.createRefreshToken(String.valueOf(kakaoId));
        response.sendRedirect("http://localhost:3000/?success=true&accessToken=" + accessToken + "&refreshToken=" + refreshToken);
    }
}

