package com.example.travel.user.config;

import com.example.travel.user.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Getter
public class AuthenticatedUser implements UserDetails, OAuth2User {
    private final User user;
    private Map<String, Object> attributes;

    // userDetailsService에서 리턴값을 사용
    public AuthenticatedUser(User user) {
        this.user = user;
    }

    // oauth2UserService에서 리턴값을 사용
    public AuthenticatedUser(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    // 사용자에 권한 정보를 리턴
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collect = new ArrayList<>();
        collect.add(new SimpleGrantedAuthority(user.getRole().name()));
        return collect;
    }

    // 사용자 비밀번호를 리턴
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // 사용자 이름을 리턴
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return user.getUsername();
    }

}