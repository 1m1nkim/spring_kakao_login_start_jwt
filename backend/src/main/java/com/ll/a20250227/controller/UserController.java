package com.ll.a20250227.controller;

import com.ll.a20250227.entity.User;
import com.ll.a20250227.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 현재 로그인된 사용자 정보를 반환합니다.
     */
    @GetMapping("/user")
    public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
        }

        try {
            // OAuth2User에서 ID 추출
            Long userId = Long.valueOf(oAuth2User.getAttribute("id").toString());

            // 데이터베이스에서 사용자 정보 조회
            Optional<User> userOptional = userRepository.findById(userId);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                Map<String, Object> response = new HashMap<>();
                response.put("id", user.getId());
                response.put("nickname", user.getNickname());
                response.put("email", user.getEmail());
                response.put("profileImageUrl", user.getProfileImageUrl());
                return ResponseEntity.ok(response);
            } else {
                // 데이터베이스에 없는 경우 카카오 사용자 정보에서 추출
                Map<String, Object> attributes = oAuth2User.getAttributes();
                Map<String, Object> response = new HashMap<>();
                response.put("id", attributes.get("id"));

                if (attributes.containsKey("kakao_account")) {
                    Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

                    // 이메일 정보 추출 (email_needs_agreement가 null이거나 false인 경우)
                    if (kakaoAccount.containsKey("email")) {
                        Object emailNeedsAgreementObj = kakaoAccount.get("email_needs_agreement");
                        Boolean emailNeedsAgreement = emailNeedsAgreementObj != null ? (Boolean) emailNeedsAgreementObj : false;
                        if (!emailNeedsAgreement) {
                            response.put("email", kakaoAccount.get("email"));
                        }
                    }

                    // 프로필 정보 추출 (profile_needs_agreement가 null이거나 false인 경우)
                    if (kakaoAccount.containsKey("profile")) {
                        Object profileNeedsAgreementObj = kakaoAccount.get("profile_needs_agreement");
                        Boolean profileNeedsAgreement = profileNeedsAgreementObj != null ? (Boolean) profileNeedsAgreementObj : false;
                        if (!profileNeedsAgreement) {
                            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                            response.put("nickname", profile.get("nickname"));
                            response.put("profileImageUrl", profile.get("profile_image_url"));
                        }
                    }
                }

                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("사용자 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 로그아웃 처리를 합니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }
}
