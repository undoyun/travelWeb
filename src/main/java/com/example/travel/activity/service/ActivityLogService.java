package com.example.travel.activity.service;

import com.example.travel.activity.dto.ActivityLogDTO;
import com.example.travel.activity.model.ActivityLog;
import com.example.travel.activity.model.ActivityType;
import com.example.travel.activity.repository.ActivityLogRepository;
import com.example.travel.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    /**
     * 활동 로그를 저장합니다.
     * @param activityLog 활동 로그 객체
     * @return 저장된 활동 로그
     */
    @Transactional
    public ActivityLog saveActivityLog(ActivityLog activityLog) {
        return activityLogRepository.save(activityLog);
    }

    /**
     * 로그인 활동을 기록합니다.
     * @param user 사용자
     * @param ipAddress IP 주소
     * @return 저장된 활동 로그
     */
    @Transactional
    public ActivityLog logLogin(User user, String ipAddress) {
        ActivityLog activityLog = ActivityLog.createLoginActivity(user, ipAddress);
        return saveActivityLog(activityLog);
    }

    /**
     * 리뷰 활동을 기록합니다.
     * @param user 사용자
     * @param action 액션 (생성, 수정, 삭제 등)
     * @param reviewId 리뷰 ID
     * @return 저장된 활동 로그
     */
    @Transactional
    public ActivityLog logReviewActivity(User user, String action, Long reviewId) {
        ActivityLog activityLog = ActivityLog.createReviewActivity(user, action, reviewId);
        return saveActivityLog(activityLog);
    }

    /**
     * 일정 활동을 기록합니다.
     * @param user 사용자
     * @param action 액션 (생성, 수정, 삭제 등)
     * @param scheduleId 일정 ID
     * @return 저장된 활동 로그
     */
    @Transactional
    public ActivityLog logScheduleActivity(User user, String action, Long scheduleId) {
        ActivityLog activityLog = ActivityLog.createScheduleActivity(user, action, scheduleId);
        return saveActivityLog(activityLog);
    }

    /**
     * 댓글 활동을 기록합니다.
     * @param user 사용자
     * @param reviewId 리뷰 ID
     * @return 저장된 활동 로그
     */
    @Transactional
    public ActivityLog logCommentActivity(User user, Long reviewId) {
        ActivityLog activityLog = ActivityLog.createCommentActivity(user, reviewId);
        return saveActivityLog(activityLog);
    }

    /**
     * 문의 활동을 기록합니다.
     * @param user 사용자
     * @param action 액션 (생성, 답변 등)
     * @param inquiryId 문의 ID
     * @return 저장된 활동 로그
     */
    @Transactional
    public ActivityLog logInquiryActivity(User user, String action, Long inquiryId) {
        ActivityLog activityLog = ActivityLog.createInquiryActivity(user, action, inquiryId);
        return saveActivityLog(activityLog);
    }

    /**
     * 최근 활동 로그를 조회합니다.
     * @return 최근 활동 로그 목록
     */
    @Transactional(readOnly = true)
    public List<ActivityLogDTO> getRecentActivities() {
        List<ActivityLog> activities = activityLogRepository.findTop20ByOrderByCreatedAtDesc();
        return convertToDTO(activities);
    }

    /**
     * 특정 사용자의 활동 로그를 조회합니다.
     * @param userId 사용자 ID
     * @return 사용자의 활동 로그 목록
     */
    @Transactional(readOnly = true)
    public List<ActivityLogDTO> getUserActivities(Long userId) {
        List<ActivityLog> activities = activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return convertToDTO(activities);
    }

    /**
     * 특정 유형의 활동 로그를 조회합니다.
     * @param activityType 활동 유형
     * @return 특정 유형의 활동 로그 목록
     */
    @Transactional(readOnly = true)
    public List<ActivityLogDTO> getActivitiesByType(ActivityType activityType) {
        List<ActivityLog> activities = activityLogRepository.findByActivityTypeOrderByCreatedAtDesc(activityType);
        return convertToDTO(activities);
    }

    /**
     * 특정 기간의 활동 로그를 조회합니다.
     * @param start 시작 시간
     * @param end 종료 시간
     * @return 특정 기간의 활동 로그 목록
     */
    @Transactional(readOnly = true)
    public List<ActivityLogDTO> getActivitiesByPeriod(LocalDateTime start, LocalDateTime end) {
        List<ActivityLog> activities = activityLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        return convertToDTO(activities);
    }

    /**
     * 페이징 처리된 활동 로그를 조회합니다.
     * @param pageable 페이징 정보
     * @return 페이징 처리된 활동 로그 목록
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getActivitiesPage(Pageable pageable) {
        Page<ActivityLog> activitiesPage = activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return activitiesPage.map(this::convertToDTO);
    }

    // 엔티티를 DTO로 변환
    private List<ActivityLogDTO> convertToDTO(List<ActivityLog> activities) {
        return activities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ActivityLogDTO convertToDTO(ActivityLog activityLog) {
        return ActivityLogDTO.builder()
                .id(activityLog.getId())
                .userId(activityLog.getUser().getId())
                .userName(activityLog.getUser().getUsername())
                .activityType(activityLog.getActivityType())
                .description(activityLog.getDescription())
                .targetId(activityLog.getTargetId())
                .targetType(activityLog.getTargetType())
                .createdAt(activityLog.getCreatedAt())
                .build();
    }
} 