package com.example.travel.admin.controller;

import com.example.travel.admin.dto.ScheduleSummaryDTO;
import com.example.travel.admin.dto.ReviewSummaryDTO;
import com.example.travel.counseling.service.InquiryService;
import com.example.travel.review.service.ReviewService;
import com.example.travel.schedule.service.ScheduleService;
import com.example.travel.user.service.UserService;
import com.example.travel.activity.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 관리자 페이지(대시보드 등) 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

	private final UserService userService;
	private final ScheduleService scheduleService;
	private final ReviewService reviewService;
	private final InquiryService inquiryService;
	private final ActivityLogService activityLogService;

	/**
	 * 관리자 매인 페이지 컨트롤러
	 * 
	 * @param model
	 * @return
	 */
	@GetMapping("")
	public String getAdminMainPage(Model model) {
		// 사용자 수
		long totalUsers = userService.getTotalUserCount();

		// 일정 수, 리뷰 수, 문의 수
		long totalSchedules = scheduleService.getTotalSchedulesCount();
		long totalReviews = reviewService.getTotalReviewsCount();
		long totalInquiries = inquiryService.getTotalInquiriesCount();

		// 미답변 문의 수
		long unansweredInquiries = inquiryService.getUnansweredInquiriesCount();

		// 최근 생성된 일정
		List<ScheduleSummaryDTO> recentSchedules = scheduleService.getRecentSchedules(5);
		List<ReviewSummaryDTO> recentReviews = reviewService.getRecentReviews(5);

		// 최근 활동 로그
		var recentActivities = activityLogService.getRecentActivities();

		// 모델에 데이터 추가
		model.addAttribute("totalUsers", totalUsers);
		model.addAttribute("totalSchedules", totalSchedules);
		model.addAttribute("totalReviews", totalReviews);
		model.addAttribute("totalInquiries", totalInquiries);
		model.addAttribute("unansweredInquiries", unansweredInquiries);
		model.addAttribute("recentSchedules", recentSchedules);
		model.addAttribute("recentReviews", recentReviews);
		model.addAttribute("recentActivities", recentActivities);

		return "admin/adminMain";
	}

	/**
	 * 관리자 활동 로그 페이지를 반환합니다.
	 * @param model 모델
	 * @param pageable 페이징 정보
	 * @return 활동 로그 페이지 뷰
	 */
	@GetMapping("/activity-logs")
	public String getActivitiesPage(Model model, @PageableDefault(size = 20) Pageable pageable) {
		var activitiesPage = activityLogService.getActivitiesPage(pageable);
		model.addAttribute("activitiesPage", activitiesPage);
		return "admin/activities";
	}
}
