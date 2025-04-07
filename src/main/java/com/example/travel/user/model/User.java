package com.example.travel.user.model;

import com.example.travel.calendar.model.Calendar;
import com.example.travel.counseling.model.Counseling;
import com.example.travel.itinerary.model.TravelPlan;
import com.example.travel.review.model.Comment;
import com.example.travel.review.model.Review;
import com.example.travel.schedule.model.Schedule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String nickname;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false, length = 10)
	private String gender;

	@Column(nullable = false)
	private LocalDate birthdate;

	@Enumerated(EnumType.STRING)
	private RoleType role; // ROLE_USER, ROLE_ADMIN 등

	@Column(nullable = false, length = 20)
	@Builder.Default
	private String status = "active"; // active, inactive, suspended

	@Column(nullable = false)
	private String provider; // 소셜 로그인 제공자 정보 (예: "google")

	@Column(name = "created_at", nullable = true, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = true)
	private LocalDateTime updatedAt;

	@Column(name = "last_login_at", nullable = true)
	private LocalDateTime lastLoginAt;

	@Column(name = "suspended_until", nullable = true)
	private LocalDateTime suspendedUntil;

	@Column(name = "login_count", nullable = true)
	@Builder.Default
	private Integer loginCount = 0;

	// 관계 매핑 추가
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Review> reviews = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Comment> comments = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Calendar> calendars = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Schedule> schedules = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Counseling> counselings = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<TravelPlan> travelPlans = new ArrayList<>();

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	// 로그인 시 호출할 메서드
	public void recordLogin() {
		this.lastLoginAt = LocalDateTime.now();
		this.loginCount = (this.loginCount == null) ? 1 : this.loginCount + 1;
	}
}