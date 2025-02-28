package com.ll.b20250227.security;

import com.ll.b20250227.jwt.JwtAuthenticationFilter;
import com.ll.b20250227.jwt.JwtProvider;
import com.ll.b20250227.oauth.CustomOAuth2UserService;
import com.ll.b20250227.oauth.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    public SecurityConfig(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomOAuth2UserService customOAuth2UserService,
                                                   OAuth2AuthorizationRequestResolver authorizationRequestResolver,
                                                   OAuth2LoginSuccessHandler oAuthLoginSuccessHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        // H2 콘솔 사용 위해 FrameOptions 비활성화
                        .frameOptions(frameOptions -> frameOptions.disable())
                )
                // 세션을 사용하지 않고 stateless하게 만듭니다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/oauth2/authorization/**", "/h2-console/**", "/api/refresh").permitAll()
                        .requestMatchers("/api/logout").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(oAuthLoginSuccessHandler)
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

        // JWT 인증 필터 추가 (UsernamePasswordAuthenticationFilter 전에 등록)
        http.addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // NextJS 프론트엔드 주소
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // 쿠키를 포함한 요청 허용 (필요한 경우)
        configuration.setAllowCredentials(true);
        // 인증 헤더 노출
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver requestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");

        requestResolver.setAuthorizationRequestCustomizer(builder -> {
            builder.attributes(attrs -> {
                String registrationId = (String) attrs.get("registration_id");
                if ("kakao".equals(registrationId)) {
                    builder.additionalParameters(params -> {
                        params.put("prompt", "login");
                    });
                }
            });
        });

        return requestResolver;
    }
}
