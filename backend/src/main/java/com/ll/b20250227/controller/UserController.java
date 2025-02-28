package com.ll.b20250227.controller;

import com.ll.b20250227.entity.User;
import com.ll.b20250227.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    @GetMapping("/user")
    public ResponseEntity<?> getUserInfo() {
        // JWT 필터가 인증에 성공했다면, SecurityContext에 UserDetails가 들어있음
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
        }

        // principal을 UserDetails로 캐스팅 (혹은 CustomUserDetails)
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
        }

        //userDetails.getUsername() = subject(카카오 ID)
        String userIdStr = userDetails.getUsername();
        Long userId = Long.valueOf(userIdStr);

        // DB 조회
        var userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
        }

        User user = userOptional.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("nickname", user.getNickname());
        response.put("email", user.getEmail());
        response.put("profileImageUrl", user.getProfileImageUrl());

        return ResponseEntity.ok(response);
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }
}
