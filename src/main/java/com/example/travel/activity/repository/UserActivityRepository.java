package com.example.travel.activity.repository;

import com.example.travel.activity.model.ActivityType;
import com.example.travel.activity.model.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    // 특정 사용자의 활동 조회
    List<UserActivity> findByUserId(Long userId);

    // 페이징을 적용한 특정 사용자의 활동 조회
    Page<UserActivity> findByUserId(Long userId, Pageable pageable);

    // 특정 활동 유형에 대한 모든 활동 조회
    Page<UserActivity> findByActivityType(ActivityType activityType, Pageable pageable);

    // 특정 사용자의 특정 활동 유형에 대한 활동 조회
    Page<UserActivity> findByUserIdAndActivityType(Long userId, ActivityType activityType, Pageable pageable);

    // 특정 기간 내의 활동 조회
    Page<UserActivity> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // 특정 사용자의 특정 기간 내 활동 조회
    Page<UserActivity> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable);

    // 키워드를 포함하는 설명이나 상세 정보를 가진 활동 검색
    @Query("SELECT a FROM UserActivity a WHERE a.description LIKE %:keyword% OR a.details LIKE %:keyword%")
    Page<UserActivity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 최근 활동 조회 (전체 사용자)
    @Query("SELECT a FROM UserActivity a ORDER BY a.createdAt DESC")
    Page<UserActivity> findRecentActivities(Pageable pageable);

    // 특정 타겟 ID와 타입에 관련된 활동 조회
    Page<UserActivity> findByTargetIdAndTargetType(String targetId, String targetType, Pageable pageable);
}