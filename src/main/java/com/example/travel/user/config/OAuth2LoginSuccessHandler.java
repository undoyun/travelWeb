package com.example.travel.user.config;

import com.example.travel.user.model.User;
import com.example.travel.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ApplicationContext applicationContext;
    private UserService userService;

    public OAuth2LoginSuccessHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // Lazy 로딩 방식으로 UserService 가져오기
    private UserService getUserService() {
        if (userService == null) {
            userService = applicationContext.getBean(UserService.class);
        }
        return userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        User user = authenticatedUser.getUser();

        log.info("소셜 로그인 성공: {}", user.getUsername());

        // 닉네임, 생년월일 등 필수 정보 없으면 추가 정보 입력 모달로
        if (user.getNickname() == null || user.getGender() == null || user.getBirthdate() == null) {
            log.info("추가 정보 입력 필요: {}", user.getUsername());
            HttpSession session = request.getSession();
            session.setAttribute("tempUser", user);

            // 모달 표시를 위한 URL 파라미터를 추가하여 로그인 페이지로 리다이렉트
            response.sendRedirect("/users/login?showModal=true");
        } else {
            log.info("모든 필수 정보 있음, 로그인 성공 페이지로 이동: {}", user.getUsername());

            // 로그인 횟수 기록
            user.recordLogin();
            getUserService().updateUser(user.getId(), user);

            response.sendRedirect("/users/login-success");
        }
    }
}
