package com.ll.a20250227.security;

import com.ll.a20250227.oauth.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService, OAuth2AuthorizationRequestResolver authorizationRequestResolver) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        // H2 콘솔 사용 위해 FrameOptions 비활성화
                        .frameOptions(frameOptions -> frameOptions.disable())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/oauth2/authorization/**").permitAll()
                        .requestMatchers("/api/logout").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        // 인증이 안 된 상태에서 /api/** 호출 시 401을 응답하도록 설정
                        .authenticationEntryPoint((request, response, authException) -> {
                            // AJAX 요청이면 401을 보내고, 그 외는 /login으로 리다이렉트하도록 구분도 가능
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("http://localhost:3000/?success=true", true)
                        .failureUrl("http://localhost:3000/?error=true")
                        .authorizationEndpoint(endpoint ->
                                endpoint.authorizationRequestResolver(authorizationRequestResolver)
                        )
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("http://localhost:3000/?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // NextJS 프론트엔드 주소
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // 쿠키를 포함한 요청 허용 (인증 정보 전송에 필요)
        configuration.setAllowCredentials(true);
        // 인증 헤더 노출
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {

        DefaultOAuth2AuthorizationRequestResolver requestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");

        requestResolver.setAuthorizationRequestCustomizer(builder -> {
            // builder.attributes(...)로 registrationId를 확인
            builder.attributes(attrs -> {
                // registrationId 추출
                String registrationId = (String) attrs.get("registration_id");
                // import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REGISTRATION_ID;
                // 이렇게 상수로 가져와도 됨

                if ("kakao".equals(registrationId)) {
                    // 특정 클라이언트(Kakao)인 경우 prompt=login 추가
                    builder.additionalParameters(params -> {
                        params.put("prompt", "login");
                    });
                }
            });
        });

        return requestResolver;
    }
}
