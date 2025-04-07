package com.example.travel.itinerary.controller;

import com.example.travel.exception.BadRequestException;
import com.example.travel.exception.ResourceNotFoundException;
import com.example.travel.itinerary.dto.TravelPlanDTO;
import com.example.travel.itinerary.service.TravelPlanService;
import com.example.travel.schedule.dto.ScheduleDTO;
import com.example.travel.review.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/itineraries")
public class TravelPlanController {

    @Autowired
    private TravelPlanService travelPlanService;

    @Autowired
    private ReviewService reviewService;

    /**
     * 메인 페이지를 표시합니다.
     */
    @GetMapping("/itineraryMain")
    public String itineraryMain(Model model) {
        // 인기 리뷰 상위 3개를 가져옴
        model.addAttribute("topReviews", reviewService.getTopReviews(3));
        return "itineraries/itineraryMain";
    }

    /**
     * 여행 계획 생성 폼을 표시합니다.
     */
    @GetMapping("/itineraryCreate")
    public String itineraryCreate(Model model) {
        model.addAttribute("travelPlan", new TravelPlanDTO());
        return "itineraries/itineraryCreate";
    }

    /**
     * 여행 계획을 생성하고 GPT로 일정을 생성합니다.
     */
    @PostMapping("/itineraryCreate")
    public String createPlanSubmit(@ModelAttribute TravelPlanDTO travelPlanDTO,
            BindingResult bindingResult,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        // 유효성 검증 오류가 있는 경우
        if (bindingResult.hasErrors()) {
            log.warn("여행 계획 생성 폼 유효성 검증 실패: {}", bindingResult.getAllErrors());
            return "itineraries/itineraryCreate";
        }

        try {
            // 사용자 ID 설정
            travelPlanDTO.setUserId(authentication.getName());

            // 여행 계획 생성
            TravelPlanDTO savedPlan = travelPlanService.createPlan(travelPlanDTO);

            // 모델에 데이터 추가 - 로그 추가
            log.info("itineraryJson: {}", savedPlan.getItinerary());
            model.addAttribute("itineraryJson", savedPlan.getItinerary());
            model.addAttribute("planId", savedPlan.getPlanId());

            return "itineraries/itineraryEdit";

        } catch (BadRequestException e) {
            // 입력값 오류
            log.warn("여행 계획 생성 중 입력값 오류: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "itineraries/itineraryCreate";

        } catch (Exception e) {
            // 기타 오류
            log.error("여행 계획 생성 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "여행 계획 생성 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/itineraries/itineraryCreate";
        }
    }

    /**
     * 편집된 여행 계획을 확정합니다.
     */
    @PostMapping("/confirm")
    public String confirmPlan(@RequestParam("editedItinerary") String editedItinerary,
            @RequestParam("planId") Long planId,
            @RequestParam("planName") String planName,
            @RequestParam("planPhoto") String planPhoto,
            @RequestParam("planDescription") String planDescription,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            String userId = authentication.getName();

            // 입력값 기본 검증
            if (editedItinerary == null || editedItinerary.trim().isEmpty()) {
                throw new BadRequestException("편집된 일정 정보가 없습니다.");
            }

            if (planName == null || planName.trim().isEmpty()) {
                throw new BadRequestException("여행 이름은 필수 입력 항목입니다.");
            }

            // 여행 계획 확정
            ScheduleDTO schedule = travelPlanService.confirmPlan(
                    planId, editedItinerary, userId, planName, planPhoto, planDescription);

            // 중요: 모델에 데이터를 추가하고 결과 페이지로 이동
            model.addAttribute("scheduleItem", schedule);
            return "itineraries/itineraryResult";

        } catch (ResourceNotFoundException e) {
            // 리소스를 찾을 수 없는 경우
            log.warn("여행 계획 확정 중 리소스 없음: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/itineraries/itineraryMain";

        } catch (BadRequestException e) {
            // 입력값 오류
            log.warn("여행 계획 확정 중 입력값 오류: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("planId", planId);
            model.addAttribute("editedItinerary", editedItinerary);
            return "itineraries/itineraryEdit";

        } catch (Exception e) {
            // 기타 오류
            log.error("여행 계획 확정 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "여행 계획 확정 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/itineraries/itineraryEdit?planId=" + planId;
        }
    }

    /**
     * 추가 정보를 업데이트합니다.
     */
    @PostMapping("/updateAdditionalInfo")
    public String updateAdditionalInfo(@RequestParam("scheduleItemId") Long scheduleId,
            @RequestParam("planName") String planName,
            @RequestParam("planPhoto") String planPhoto,
            @RequestParam("planDescription") String planDescription,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            // 입력값 기본 검증
            if (planName == null || planName.trim().isEmpty()) {
                throw new BadRequestException("여행 이름은 필수 입력 항목입니다.");
            }

            // 추가 정보 업데이트
            ScheduleDTO updated = travelPlanService.updateAdditionalInfo(
                    scheduleId, planName, planPhoto, planDescription);

            model.addAttribute("scheduleItem", updated);
            return "itineraries/itineraryResults";

        } catch (ResourceNotFoundException e) {
            // 리소스를 찾을 수 없는 경우
            log.warn("추가 정보 업데이트 중 리소스 없음: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/schedule/schedule";

        } catch (BadRequestException e) {
            // 입력값 오류
            log.warn("추가 정보 업데이트 중 입력값 오류: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("scheduleItemId", scheduleId);
            return "itineraries/itineraryResult";

        } catch (Exception e) {
            // 기타 오류
            log.error("추가 정보 업데이트 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "추가 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/schedule/schedule";
        }
    }

    /**
     * 여행 계획 목록을 표시합니다.
     */
    @GetMapping("/myPlans")
    public String myPlans(Model model, Authentication authentication) {
        try {
            String userId = authentication.getName();
            model.addAttribute("plans", travelPlanService.getUserPlans(userId));
            return "itineraries/myPlans";
        } catch (Exception e) {
            log.error("여행 계획 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "여행 계획 목록을 불러오는 중 오류가 발생했습니다.");
            return "itineraries/myPlans";
        }
    }

    /**
     * 여행 계획 상세 페이지를 표시합니다.
     */
    @GetMapping("/itineraryDetail/{planId}")
    public String viewItinerary(@PathVariable Long planId, Model model) {
        try {
            TravelPlanDTO plan = travelPlanService.getPlan(planId);
            model.addAttribute("plan", plan);
            return "itineraries/itineraryDetail";
        } catch (Exception e) {
            model.addAttribute("error", "여행 계획을 찾을 수 없습니다.");
            return "redirect:/itineraries/itineraryMain";
        }
    }

    /**
     * 사용자의 여행 계획 목록 페이지를 표시합니다.
     */
    @GetMapping("/myItineraries")
    public String myItineraries(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("plans", travelPlanService.getUserPlans(authentication.getName()));
        }
        return "itineraries/myItineraries";
    }
}
