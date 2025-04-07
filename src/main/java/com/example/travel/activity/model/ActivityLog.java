package com.example.travel.activity.model;

import com.example.travel.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType;

    @Column(nullable = false, length = 512)
    private String description;

    // 활동 대상의 ID (예: 리뷰 ID, 일정 ID 등)
    private Long targetId;

    // 활동 대상의 타입 (예: "review", "schedule", "inquiry" 등)
    @Column(length = 50)
    private String targetType;

    // IP 주소 등 추가 정보
    @Column(length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // 활동 생성 펙토리 메서드
    public static ActivityLog createLoginActivity(User user, String ipAddress) {
        return ActivityLog.builder()
                .user(user)
                .activityType(ActivityType.LOGIN)
                .description(user.getUsername() + "님이 로그인했습니다.")
                .ipAddress(ipAddress)
                .build();
    }

    public static ActivityLog createLogoutActivity(User user, String ipAddress) {
        return ActivityLog.builder()
                .user(user)
                .activityType(ActivityType.LOGOUT)
                .description(user.getUsername() + "님이 로그아웃했습니다.")
                .ipAddress(ipAddress)
                .build();
    }

    public static ActivityLog createReviewActivity(User user, String action, Long reviewId) {
        return ActivityLog.builder()
                .user(user)
                .activityType(ActivityType.REVIEW)
                .description(user.getUsername() + "님이 리뷰를 " + action + "했습니다.")
                .targetId(reviewId)
                .targetType("review")
                .build();
    }

    public static ActivityLog createScheduleActivity(User user, String action, Long scheduleId) {
        return ActivityLog.builder()
                .user(user)
                .activityType(ActivityType.SCHEDULE)
                .description(user.getUsername() + "님이 일정을 " + action + "했습니다.")
                .targetId(scheduleId)
                .targetType("schedule")
                .build();
    }

    public static ActivityLog createCommentActivity(User user, Long reviewId) {
        return ActivityLog.builder()
                .user(user)
                .activityType(ActivityType.COMMENT)
                .description(user.getUsername() + "님이 댓글을 작성했습니다.")
                .targetId(reviewId)
                .targetType("review")
                .build();
    }

    public static ActivityLog createInquiryActivity(User user, String action, Long inquiryId) {
        return ActivityLog.builder()
                .user(user)
                .activityType(ActivityType.INQUIRY)
                .description(user.getUsername() + "님이 문의를 " + action + "했습니다.")
                .targetId(inquiryId)
                .targetType("inquiry")
                .build();
    }

    public static ActivityLog createRegisterActivity(User user, String ipAddress) {
        return ActivityLog.builder()
                .user(user)
                .activityType(ActivityType.REGISTER)
                .description(user.getUsername() + "님이 회원가입했습니다.")
                .ipAddress(ipAddress)
                .build();
    }

    public static ActivityLog createProfileUpdateActivity(User user) {
        return ActivityLog.builder()
                .user(user)
                .activityType(ActivityType.PROFILE_UPDATE)
                .description(user.getUsername() + "님이 프로필을 수정했습니다.")
                .build();
    }

    public static ActivityLog createPasswordChangeActivity(User user) {
        return ActivityLog.builder()
                .user(user)
                .activityType(ActivityType.PASSWORD_CHANGE)
                .description(user.getUsername() + "님이 비밀번호를 변경했습니다.")
                .build();
    }
}