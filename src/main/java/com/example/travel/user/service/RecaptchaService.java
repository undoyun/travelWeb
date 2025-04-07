package com.example.travel.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class RecaptchaService {

    @Value("${google.recaptcha.secret}")
    private String recaptchaSecret;

    private final WebClient webClient;

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public boolean verify(String recaptchaResponse) {
        if (recaptchaResponse == null || recaptchaResponse.isEmpty()) {
            return false;
        }
        RecaptchaResponse result = webClient
                .post()
                .uri(RECAPTCHA_VERIFY_URL + "?secret={secret}&response={response}", recaptchaSecret, recaptchaResponse)
                .retrieve()
                .bodyToMono(RecaptchaResponse.class)
                .block();

        return result != null && result.isSuccess();
    }

    // 내부 클래스로 reCAPTCHA 응답을 매핑합니다.
    public static class RecaptchaResponse {
        private boolean success;
        private String challenge_ts;
        private String hostname;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getChallenge_ts() {
            return challenge_ts;
        }

        public void setChallenge_ts(String challenge_ts) {
            this.challenge_ts = challenge_ts;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
    }
}
