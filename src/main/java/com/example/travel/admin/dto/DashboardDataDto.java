package com.example.travel.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDataDto {
    private long userCount;               // 총 사용자 수
    private long totalItineraries;        // 총 여행 일정 수
    private long weeklyCreatedItineraries;// 이번 주 신규 일정 수
    private long pendingSpotRequests;     // 명소 추가 요청
    private long unansweredInquiries;     // 미답변 1:1 문의
    private long reportedReviews;         // 신고된 리뷰
}
