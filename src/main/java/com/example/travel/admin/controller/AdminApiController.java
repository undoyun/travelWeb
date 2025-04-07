package com.example.travel.admin.controller;

import com.example.travel.activity.dto.ActivityLogDTO;
import com.example.travel.activity.service.ActivityLogService;
import com.example.travel.counseling.service.InquiryService;
import com.example.travel.review.service.ReviewService;
import com.example.travel.schedule.service.ScheduleService;
import com.example.travel.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class AdminApiController {

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final ReviewService reviewService;
    private final InquiryService inquiryService;
    private final ActivityLogService activityLogService;

    /**
     * 대시보드 요약 통계를 가져옵니다.
     * @return 요약 통계 데이터
     */
    @GetMapping("/dashboard-summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // 사용자 통계
        long totalUsers = userService.getTotalUserCount();
        summary.put("totalUsers", totalUsers);
        
        // 일정 통계
        long totalSchedules = scheduleService.getTotalSchedulesCount();
        summary.put("totalSchedules", totalSchedules);
        
        // 리뷰 통계
        long totalReviews = reviewService.getTotalReviewsCount();
        double averageRating = reviewService.getAverageRating();
        summary.put("totalReviews", totalReviews);
        summary.put("averageRating", averageRating);
        
        // 문의 통계
        long totalInquiries = inquiryService.getTotalInquiriesCount();
        long unansweredInquiries = inquiryService.getUnansweredInquiriesCount();
        summary.put("totalInquiries", totalInquiries);
        summary.put("unansweredInquiries", unansweredInquiries);
        
        return ResponseEntity.ok(summary);
    }

    /**
     * 최근 활동 로그를 가져옵니다.
     * @return 최근 활동 로그 목록
     */
    @GetMapping("/recent-activities")
    public ResponseEntity<List<ActivityLogDTO>> getRecentActivities() {
        return ResponseEntity.ok(activityLogService.getRecentActivities());
    }
} 