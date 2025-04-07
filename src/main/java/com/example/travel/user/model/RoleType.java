package com.example.travel.user.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RoleType {
	ROLE_ADMIN("최고 관리자"),
	ROLE_USER("사용자");

	private final String description;
}
