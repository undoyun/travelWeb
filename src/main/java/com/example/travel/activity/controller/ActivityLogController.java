package com.example.travel.activity.controller;

import com.example.travel.activity.dto.ActivityLogDTO;
import com.example.travel.activity.model.ActivityType;
import com.example.travel.activity.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    /**
     * 최근 활동 로그를 조회합니다.
     * @return 최근 활동 로그 목록
     */
    @GetMapping("/recent")
    public ResponseEntity<List<ActivityLogDTO>> getRecentActivities() {
        return ResponseEntity.ok(activityLogService.getRecentActivities());
    }

    /**
     * 특정 사용자의 활동 로그를 조회합니다.
     * @param userId 사용자 ID
     * @return 사용자의 활동 로그 목록
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ActivityLogDTO>> getUserActivities(@PathVariable Long userId) {
        return ResponseEntity.ok(activityLogService.getUserActivities(userId));
    }

    /**
     * 특정 유형의 활동 로그를 조회합니다.
     * @param activityType 활동 유형
     * @return 특정 유형의 활동 로그 목록
     */
    @GetMapping("/type/{activityType}")
    public ResponseEntity<List<ActivityLogDTO>> getActivitiesByType(@PathVariable ActivityType activityType) {
        return ResponseEntity.ok(activityLogService.getActivitiesByType(activityType));
    }

    /**
     * 특정 기간의 활동 로그를 조회합니다.
     * @param start 시작 시간
     * @param end 종료 시간
     * @return 특정 기간의 활동 로그 목록
     */
    @GetMapping("/period")
    public ResponseEntity<List<ActivityLogDTO>> getActivitiesByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(activityLogService.getActivitiesByPeriod(start, end));
    }

    /**
     * 페이징 처리된 활동 로그를 조회합니다.
     * @param pageable 페이징 정보
     * @return 페이징 처리된 활동 로그 목록
     */
    @GetMapping
    public ResponseEntity<Page<ActivityLogDTO>> getActivitiesPage(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getActivitiesPage(pageable));
    }
} 