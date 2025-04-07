package com.example.travel.activity.controller;

import com.example.travel.activity.model.ActivityType;
import com.example.travel.activity.model.UserActivity;
import com.example.travel.activity.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/admin/activities")
@RequiredArgsConstructor
public class AdminActivityController {

    private final UserActivityService userActivityService;

    /**
     * 모든 사용자 활동 로그를 조회합니다.
     */
    @GetMapping
    public String getAllActivities(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "type", required = false) String activityType,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        // 정렬 설정
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        // 활동 로그 조회 (필터링 적용)
        Page<UserActivity> activities;

        if (userId != null && activityType != null) {
            // 사용자 ID와 활동 유형으로 필터링
            activities = userActivityService.getActivitiesByUserIdAndType(userId,
                    ActivityType.valueOf(activityType), pageable);
        } else if (userId != null) {
            // 사용자 ID로만 필터링
            activities = userActivityService.getActivitiesByUserId(userId, pageable);
        } else if (activityType != null) {
            // 활동 유형으로만 필터링
            activities = userActivityService.getActivitiesByType(ActivityType.valueOf(activityType), pageable);
        } else if (startDate != null && endDate != null) {
            // 날짜 범위로 필터링
            activities = userActivityService.getActivitiesByDateRange(startDate, endDate, pageable);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            // 키워드로 검색
            activities = userActivityService.searchActivitiesByKeyword(keyword, pageable);
        } else {
            // 필터 없이 모든 활동 조회
            activities = userActivityService.getRecentActivities(pageable);
        }

        // 활동 유형 목록
        Map<String, String> activityTypes = Map.ofEntries(
                Map.entry("ALL", "모든 활동"),
                Map.entry("LOGIN", "로그인"),
                Map.entry("LOGOUT", "로그아웃"),
                Map.entry("TRAVEL_PLAN_CREATE", "여행 계획 생성"),
                Map.entry("TRAVEL_PLAN_UPDATE", "여행 계획 수정"),
                Map.entry("TRAVEL_PLAN_DELETE", "여행 계획 삭제"),
                Map.entry("SCHEDULE_CREATE", "일정 생성"),
                Map.entry("SCHEDULE_UPDATE", "일정 수정"),
                Map.entry("SCHEDULE_DELETE", "일정 삭제"),
                Map.entry("REVIEW_CREATE", "리뷰 작성"),
                Map.entry("REVIEW_UPDATE", "리뷰 수정"),
                Map.entry("REVIEW_DELETE", "리뷰 삭제"),
                Map.entry("COUNSELING_CREATE", "문의 작성"),
                Map.entry("COUNSELING_UPDATE", "문의 수정"),
                Map.entry("COUNSELING_DELETE", "문의 삭제"));

        // 모델에 데이터 추가
        model.addAttribute("activities", activities);
        model.addAttribute("activityTypes", activityTypes);

        // 현재 필터 상태 추가
        model.addAttribute("currentUserId", userId);
        model.addAttribute("currentType", activityType);
        model.addAttribute("currentStartDate", startDate);
        model.addAttribute("currentEndDate", endDate);
        model.addAttribute("currentKeyword", keyword);

        return "admin/activities";
    }

    /**
     * 최근 활동 데이터를 JSON 형식으로 반환하는 API 엔드포인트
     * 
     * @return 최근 활동 목록(JSON)
     */
    @GetMapping("/api/recent-activities")
    @ResponseBody
    public List<UserActivity> getRecentActivitiesJson() {
        return userActivityService.getRecentActivities(5); // 최근 5개 활동만 반환
    }
}