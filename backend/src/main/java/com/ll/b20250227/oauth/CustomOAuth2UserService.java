package com.ll.b20250227.oauth;

import com.ll.b20250227.entity.User;
import com.ll.b20250227.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 기본 OAuth2UserService를 통해 사용자 정보를 가져옴
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 제공자 정보 (kakao)
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 카카오는 ID가 Long 타입이므로 적절히 변환
        Long id = Long.valueOf(attributes.get("id").toString());

        // 사용자 정보 생성 또는 업데이트
        User user = saveOrUpdateUser(provider, id, attributes);

        // ROLE_USER 권한을 부여하는 방식
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oAuth2User.getAttributes(),
                userRequest.getClientRegistration()
                        .getProviderDetails()
                        .getUserInfoEndpoint()
                        .getUserNameAttributeName()
        );
    }

    private User saveOrUpdateUser(String provider, Long id, Map<String, Object> attributes) {
        // 카카오 사용자 정보 파싱
        String nickname = null;
        String email = null;
        String profileImageUrl = null;

        // 카카오 계정 정보 추출
        if (attributes.containsKey("kakao_account")) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

            // 1) email 처리
            // email_needs_agreement 키가 있을 수 있으므로 널 체크
            Object emailNeedsAgreementObj = kakaoAccount.get("email_needs_agreement");
            // 널이면 false 처리
            boolean emailNeedsAgreement = (emailNeedsAgreementObj instanceof Boolean) ? (Boolean) emailNeedsAgreementObj : false;

            if (kakaoAccount.containsKey("email") && !emailNeedsAgreement) {
                email = (String) kakaoAccount.get("email");
            }

            // 2) 프로필 정보 처리
            Object profileNeedsAgreementObj = kakaoAccount.get("profile_needs_agreement");
            boolean profileNeedsAgreement = (profileNeedsAgreementObj instanceof Boolean) ? (Boolean) profileNeedsAgreementObj : false;

            if (kakaoAccount.containsKey("profile") && !profileNeedsAgreement) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                nickname = (String) profile.get("nickname");
                profileImageUrl = (String) profile.get("profile_image_url");
            }
        }

        // 사용자 생성 또는 업데이트
        User user = userRepository.findById(id).orElse(new User());
        user.setId(id);
        user.setProvider(provider);

        if (nickname != null) {
            user.setNickname(nickname);
        }

        if (email != null) {
            user.setEmail(email);
        }

        if (profileImageUrl != null) {
            user.setProfileImageUrl(profileImageUrl);
        }

        return userRepository.save(user);
    }
}
