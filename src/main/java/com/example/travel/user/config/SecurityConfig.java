package com.example.travel.user.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

// 스프링 시큐리티의 전체적인 설정을 관리하는 클래스
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

	private final AuthenticationFailureHandler authenticationFailureHandler;
	private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService;
	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(request -> request
						// ✅ 정적 리소스 허용 (CSS, JS, IMG)
						.requestMatchers("/css/**", "/js/**", "/img/**", "/static/**", "/api/**").permitAll()

						// ✅ 인증없이 접근 가능한 페이지
						.requestMatchers("/", "/users/login", "/users/register", "/users/add-info", "/error").permitAll()
						
						// ✅ API 엔드포인트 중 인증 없이 접근 가능
						.requestMatchers("/api/users/check-username", "/api/users/check-nickname", "/api/users/add-info").permitAll()

						// ✅ 관리자 페이지 접근 제한
						.requestMatchers("/admin/**").hasAnyRole("ADMIN")

						// ✅ 그 외 모든 요청은 인증 필요
						.anyRequest().authenticated())

				// ✅ 폼 로그인 설정
				.formLogin(formLogin -> formLogin
						.loginPage("/users/login")
						.loginProcessingUrl("/users/login")
						.defaultSuccessUrl("/users/login-success")
						.failureHandler(authenticationFailureHandler)
						.permitAll())

				// ✅ 로그아웃 설정
				.logout(logout -> logout
						.logoutUrl("/users/logout")
						.logoutSuccessUrl("/")
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID"))

				// ✅ OAuth2 로그인 설정
				.oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo
					.userService(oAuth2UserService))
				.successHandler(oAuth2LoginSuccessHandler)
				.failureHandler(authenticationFailureHandler)
			);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// 단방향 암호화 : 패스워드 -> 암호화된 패스워드
		return new BCryptPasswordEncoder();
	}
}
