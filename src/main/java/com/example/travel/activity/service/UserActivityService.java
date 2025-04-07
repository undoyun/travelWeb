package com.example.travel.activity.service;

import com.example.travel.activity.model.ActivityType;
import com.example.travel.activity.model.UserActivity;
import com.example.travel.activity.repository.UserActivityRepository;
import com.example.travel.user.model.User;
import com.example.travel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 활동 로그를 저장합니다.
     */
    @Transactional
    public UserActivity logActivity(Long userId, ActivityType activityType, String description, String details,
            String targetId, String targetType, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자 ID: " + userId));

        UserActivity activity = UserActivity.builder()
                .user(user)
                .activityType(activityType)
                .description(description)
                .details(details)
                .targetId(targetId)
                .targetType(targetType)
                .ipAddress(ipAddress)
                .build();

        return userActivityRepository.save(activity);
    }

    /**
     * 사용자 활동 로그를 저장합니다. (간소화된 버전)
     */
    @Transactional
    public UserActivity logActivity(Long userId, ActivityType activityType, String description) {
        return logActivity(userId, activityType, description, null, null, null, null);
    }

    /**
     * 사용자 ID로 활동 로그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<UserActivity> getActivitiesByUserId(Long userId, Pageable pageable) {
        return userActivityRepository.findByUserId(userId, pageable);
    }

    /**
     * 활동 유형별로 로그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<UserActivity> getActivitiesByType(ActivityType activityType, Pageable pageable) {
        return userActivityRepository.findByActivityType(activityType, pageable);
    }

    /**
     * 사용자 ID와 활동 유형으로 로그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<UserActivity> getActivitiesByUserIdAndType(Long userId, ActivityType activityType, Pageable pageable) {
        return userActivityRepository.findByUserIdAndActivityType(userId, activityType, pageable);
    }

    /**
     * 특정 날짜의 활동 로그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<UserActivity> getActivitiesByDate(LocalDate date, Pageable pageable) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return userActivityRepository.findByCreatedAtBetween(startOfDay, endOfDay, pageable);
    }

    /**
     * 특정 기간의 활동 로그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<UserActivity> getActivitiesByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        return userActivityRepository.findByCreatedAtBetween(startDateTime, endDateTime, pageable);
    }

    /**
     * 특정 사용자의 특정 날짜 활동 로그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<UserActivity> getActivitiesByUserIdAndDate(Long userId, LocalDate date, Pageable pageable) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return userActivityRepository.findByUserIdAndCreatedAtBetween(userId, startOfDay, endOfDay, pageable);
    }

    /**
     * 키워드로 활동 로그를 검색합니다.
     */
    @Transactional(readOnly = true)
    public Page<UserActivity> searchActivitiesByKeyword(String keyword, Pageable pageable) {
        return userActivityRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * 최근 활동 로그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<UserActivity> getRecentActivities(Pageable pageable) {
        return userActivityRepository.findRecentActivities(pageable);
    }

    /**
     * 특정 대상 ID와 유형에 관련된 활동 로그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<UserActivity> getActivitiesByTarget(String targetId, String targetType, Pageable pageable) {
        return userActivityRepository.findByTargetIdAndTargetType(targetId, targetType, pageable);
    }

    /**
     * 지정된 개수만큼 최근 활동을 가져옵니다 (실시간 활동 표시용)
     * 
     * @param limit 가져올 활동 개수
     * @return 최근 활동 목록
     */
    public List<UserActivity> getRecentActivities(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userActivityRepository.findAll(pageRequest).getContent();
    }
}