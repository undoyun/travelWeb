package com.example.travel.user.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
// 인증에 실패했을 경우 실행될 핸들러 객체
@Slf4j
@Component
public class CustomAuthFailuerHandler extends SimpleUrlAuthenticationFailureHandler {
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		log.info("로그인 실패 핸들러 호출: {}", exception.getClass());
		String message = "";
		if (exception instanceof BadCredentialsException) {
//			log.info("아이디가 없거나 패스워드를 잘못 입력했습니다.");
			message = "아이디가 없거나 패스워드를 잘못 입력했습니다.";
		// 주로 사용자 인증 정보의 검색 또는 처리 과정에서 예상치 못한 내부 오류가 발생
		} else if (exception instanceof InternalAuthenticationServiceException) {
			message = "내부적으로 발생한 시스템 문제로 인해 요청을 처리할 수 없습니다. 관리자에 문의하세요";
		// 인증 과정이 요구되는 요청을 처리하는 동안 현재 요청에 대한 인증 객체가 보안 컨텍스트에 없거나
		// 유효하지 않을 경우 발생
		} else if (exception instanceof AuthenticationCredentialsNotFoundException) {
			message = "인증 요청이 거부되었습니다. 관리자에게 문의하세요";
		} else {
			message = "알 수 없는 이유로 로그인에 실패 했습니다.";
		}
		message = URLEncoder.encode(message, StandardCharsets.UTF_8);
		log.info("loginErrorMessage : {}", message);
		setDefaultFailureUrl("/users/login?loginError=true&errorMessage=" + message);

		super.onAuthenticationFailure(request, response, exception);
	}
}











