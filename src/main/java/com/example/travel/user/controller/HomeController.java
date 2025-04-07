package com.example.travel.user.controller;

import com.example.travel.review.service.ReviewService;
import com.example.travel.schedule.dto.ScheduleDTO;
import com.example.travel.schedule.service.ScheduleService;
import com.example.travel.user.config.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

	private final ScheduleService scheduleService;
	private final ReviewService reviewService;

	@GetMapping("/main")
	public String main(@AuthenticationPrincipal AuthenticatedUser authenticatedUser, Model model) {
		log.info("authenticatedUser: {}", authenticatedUser);

		// 사용자 ID 가져오기
		model.addAttribute("username", authenticatedUser.getUsername());

		// 모든 일정 가져오기
		String userId = authenticatedUser.getUser().getId().toString();
		List<ScheduleDTO> allSchedules = scheduleService.getSchedulesByUserId(userId);

		// 오늘 날짜
		LocalDate today = LocalDate.now();

		// 다가오는 여행 필터링 (시작일이 오늘 이후인 경우)
		List<ScheduleDTO> upcomingTrips = allSchedules.stream()
				.filter(schedule -> LocalDate.parse(schedule.getStartDate()).isAfter(today) ||
						LocalDate.parse(schedule.getStartDate()).isEqual(today))
				.collect(Collectors.toList());

		// 종료된 여행 필터링 (종료일이 오늘 이전인 경우)
		List<ScheduleDTO> completedTrips = allSchedules.stream()
				.filter(schedule -> LocalDate.parse(schedule.getEndDate()).isBefore(today))
				.collect(Collectors.toList());

		// 각 완료된 여행에 대한 리뷰 작성 여부 확인
		completedTrips.forEach(trip -> {
			boolean hasReview = reviewService.hasReviewForSchedule(trip.getId(), userId);
			trip.setHasReview(hasReview);
		});

		// 모델에 데이터 추가
		model.addAttribute("upcomingTripsList", upcomingTrips);
		model.addAttribute("completedTripsList", completedTrips);
		
		// 총 여행 수 추가
		model.addAttribute("totalTrips", allSchedules.size());
		
		// 다가오는 여행 수 추가
		model.addAttribute("upcomingTrips", upcomingTrips.size());
		
		// 방문 장소 수 추가 - 여행 일정 기준으로 계산 (모든 일정 합계)
		model.addAttribute("visitedPlaces", allSchedules.size());
		
		// 다음 여행까지 남은 일수 계산
		if (!upcomingTrips.isEmpty()) {
			ScheduleDTO nextTrip = upcomingTrips.stream()
					.min((t1, t2) -> LocalDate.parse(t1.getStartDate()).compareTo(LocalDate.parse(t2.getStartDate())))
					.orElse(null);
			
			if (nextTrip != null) {
				long daysToNext = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.parse(nextTrip.getStartDate()));
				model.addAttribute("daysToNextTrip", daysToNext);
			}
		}

		return "main";
	}

	@GetMapping
	public String home(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		log.info("authenticatedUser: {}", authenticatedUser);
		return "index";
	}
}
