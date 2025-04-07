package com.example.travel.user.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;

@ControllerAdvice
public class OAuth2ExceptionHandler {

    @ExceptionHandler(OAuth2AuthenticationRedirectException.class)
    public void handleRedirect(HttpServletResponse response, OAuth2AuthenticationRedirectException e) throws IOException {
        response.sendRedirect(e.getRedirectUrl());
    }
}
