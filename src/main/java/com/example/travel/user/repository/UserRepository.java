package com.example.travel.user.repository;

import com.example.travel.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
	Optional<User> findByUsername(String username);
	Optional<User> findByNickname(String nickname);
	Optional<User> findByEmail(String email);
	
	// 상태별 사용자 수 카운트
	long countByStatus(String status);
}
