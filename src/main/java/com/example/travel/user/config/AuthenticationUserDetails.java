package com.example.travel.user.config;

import com.example.travel.user.model.User;
import com.example.travel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/*
 * 1. 로그인 페이지에서 로그인 시도를 하면
 * 2. UserDetailsService 에서 로그인 시도를 가로챈다.
 * 3. loadUserByUsername 메소드를 실행한다.
 * 4. 파라미터로 받은 username에 해당하는 유저를 찾아서 UserDetails 타입의 객체를 리턴한다.
 */
@RequiredArgsConstructor
@Service
public class AuthenticationUserDetails implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

		// 비밀번호는 로그에 남기지 않음
		return new AuthenticatedUser(user);
	}

}
