package com.example.travel.user.config;

public class OAuth2AuthenticationRedirectException extends RuntimeException {
    private final String redirectUrl;

    public OAuth2AuthenticationRedirectException(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }
}
