package com.example.travel.activity.repository;

import com.example.travel.activity.model.ActivityLog;
import com.example.travel.activity.model.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // 최근 활동 로그 조회
    List<ActivityLog> findTop20ByOrderByCreatedAtDesc();

    // 특정 사용자의 활동 로그 조회
    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 유형의 활동 로그 조회
    List<ActivityLog> findByActivityTypeOrderByCreatedAtDesc(ActivityType activityType);

    // 특정 기간의 활동 로그 조회
    List<ActivityLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    // 페이징 처리된 활동 로그 조회
    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 특정 사용자의 페이징 처리된 활동 로그 조회
    Page<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 특정 유형의 페이징 처리된 활동 로그 조회
    Page<ActivityLog> findByActivityTypeOrderByCreatedAtDesc(ActivityType activityType, Pageable pageable);
}