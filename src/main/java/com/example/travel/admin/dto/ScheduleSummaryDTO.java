package com.example.travel.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 대시보드에 표시할 여행 일정 요약 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSummaryDTO {
    private Long id;
    private String title;
    private String userName;
    private int duration;
    private LocalDateTime createdAt;
}