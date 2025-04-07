package com.example.travel.user.config;

import com.example.travel.user.model.RoleType;
import com.example.travel.user.model.User;
import com.example.travel.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomOAuthUserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("✅ [CustomOAuthUserService] loadUser 실행");

        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("✅ [oAuth2User.getAttributes()] {}", oAuth2User.getAttributes());

        // 소셜에서 받아온 정보
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String provider = userRequest.getClientRegistration().getRegistrationId();

        if (email == null) {
            throw new OAuth2AuthenticationException("이메일 정보를 가져올 수 없습니다.");
        }

        // 기존 유저 확인
        Optional<User> findUser = userRepository.findByUsername(email);
        if (findUser.isPresent()) {
            // 기존 유저 → 로그인 처리
            log.info("✅ [기존 유저 로그인] {}", email);
            User user = findUser.get();
            return new AuthenticatedUser(user, oAuth2User.getAttributes());
        } else {
            // 신규 유저 → 임시 계정 생성 후 로그인 처리
            // 추가 정보는 OAuth2LoginSuccessHandler에서 처리
            log.info("🆕 [신규 유저 - 추가 정보 입력 필요] {}", email);

            // 임시 비밀번호 생성 (암호화는 UserService에서 수행)
            String tempPassword = UUID.randomUUID().toString();

            User tempUser = User.builder()
                    .username(email)
                    .email(email)
                    .name(name != null ? name : "임시사용자")
                    .password(tempPassword)
                    .provider(provider)
                    .role(RoleType.ROLE_USER)
                    .build();

            // 세션에 저장
            HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest().getSession();
            session.setAttribute("tempUser", tempUser);

            // 정상적인 로그인 처리 (실제 추가 정보 입력은 handler에서 처리)
            return new AuthenticatedUser(tempUser, oAuth2User.getAttributes());
        }
    }
}
