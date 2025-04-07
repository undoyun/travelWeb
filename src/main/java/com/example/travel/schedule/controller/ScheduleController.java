package com.example.travel.schedule.controller;

import com.example.travel.exception.ResourceNotFoundException;
import com.example.travel.schedule.dto.ScheduleDTO;
import com.example.travel.schedule.service.ScheduleService;
import com.example.travel.user.model.User;
import com.example.travel.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    private final UserService userService;

    /**
     * 사용자의 일정 목록 페이지를 표시합니다.
     */
    @GetMapping("/schedule")
    public String userMyPage(Model model, Authentication authentication) {
        try {
            if (authentication != null) {
                String userId = authentication.getName();
                try {
                    User user = userService.findByUsername(userId);
                    model.addAttribute("user", user);
                } catch (Exception e) {
                    log.error("사용자 정보 조회 실패: {}", e.getMessage(), e);
                    // 사용자 정보 조회 실패 시 빈 객체 제공
                    model.addAttribute("user", new User());
                }
                
                List<ScheduleDTO> schedules = scheduleService.getSchedulesByUserId(userId);
                log.info("사용자 {}의 일정 {}개 조회됨", userId, schedules.size());
                model.addAttribute("schedules", schedules);
            } else {
                log.warn("인증 정보가 없습니다. 빈 데이터를 표시합니다.");
                model.addAttribute("user", new User());
                model.addAttribute("schedules", List.of());
            }
            return "schedule/schedule";
        } catch (Exception e) {
            log.error("일정 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "일정 목록을 불러오는 중 오류가 발생했습니다.");
            // 에러 발생 시에도 최소한의 데이터를 모델에 추가
            model.addAttribute("user", new User());
            model.addAttribute("schedules", List.of());
            return "schedule/schedule";
        }
    }

    /**
     * 대문자 'P'를 사용한 경로에 대한 별도 매핑(URL 대소문자 구분 문제 해결)
     */
    @GetMapping("/schedules")
    public String userMyPageAlternate(Model model, Authentication authentication) {
        return userMyPage(model, authentication);
    }

    /**
     * 특정 일정의 상세 정보 페이지를 표시합니다.
     */
    @GetMapping("/details/{scheduleId}")
    public String scheduleDetails(@PathVariable(name = "scheduleId") Long scheduleId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            String userId = authentication.getName();
            // 권한 확인이 포함된 일정 조회
            ScheduleDTO schedule = scheduleService.getScheduleWithAuth(scheduleId, userId);
            log.info("일정 ID {} 상세 정보 조회됨", scheduleId);
            model.addAttribute("scheduleItem", schedule);
            return "schedule/scheduleDetails";
        } catch (ResourceNotFoundException e) {
            log.warn("일정 상세 조회 중 리소스 없음: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "요청하신 일정을 찾을 수 없습니다.");
            return "redirect:/schedule/schedule";
        } catch (Exception e) {
            log.error("일정 상세 조회 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "일정 정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/schedule/schedule";
        }
    }

    /**
     * 일정을 삭제합니다.
     */
    @PostMapping("/delete/{scheduleId}")
    public String deleteSchedule(@PathVariable(name = "scheduleId") Long scheduleId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String userId = authentication.getName();
            scheduleService.deleteSchedule(scheduleId, userId);
            log.info("일정 ID {} 삭제됨", scheduleId);
            redirectAttributes.addFlashAttribute("successMessage", "일정이 성공적으로 삭제되었습니다.");
            return "redirect:/schedule/schedule";
        } catch (ResourceNotFoundException e) {
            log.warn("일정 삭제 중 리소스 없음: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "요청하신 일정을 찾을 수 없습니다.");
            return "redirect:/schedule/schedule";
        } catch (Exception e) {
            log.error("일정 삭제 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "일정 삭제 중 오류가 발생했습니다.");
            return "redirect:/schedule/schedule";
        }
    }

    /**
     * 일정 수정 페이지를 표시합니다.
     */
    @GetMapping("/edit/{scheduleId}")
    public String scheduleEdit(@PathVariable(name = "scheduleId") Long scheduleId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            String userId = authentication.getName();
            // 권한 확인이 포함된 일정 조회
            ScheduleDTO schedule = scheduleService.getScheduleWithAuth(scheduleId, userId);
            log.info("일정 ID {} 수정 페이지 로드됨", scheduleId);
            model.addAttribute("scheduleItem", schedule);
            return "schedule/scheduleEdit";
        } catch (ResourceNotFoundException e) {
            log.warn("일정 수정 페이지 로드 중 리소스 없음: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "요청하신 일정을 찾을 수 없습니다.");
            return "redirect:/schedule/schedule";
        } catch (Exception e) {
            log.error("일정 수정 페이지 로드 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "일정 정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/schedule/schedule";
        }
    }

    /**
     * 수정된 일정을 저장합니다.
     */
    @PostMapping("/edit/{scheduleId}")
    public String updateSchedule(@PathVariable(name = "scheduleId") Long scheduleId,
            @RequestParam("editedItinerary") String editedItinerary,
            @RequestParam("planName") String planName,
            @RequestParam("planPhoto") String planPhoto,
            @RequestParam("planDescription") String planDescription,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String userId = authentication.getName();
            scheduleService.updateSchedule(scheduleId, userId, editedItinerary, planName, planPhoto, planDescription);
            log.info("일정 ID {} 수정 완료됨", scheduleId);
            redirectAttributes.addFlashAttribute("successMessage", "일정이 성공적으로 수정되었습니다.");
            return "redirect:/schedule/details/" + scheduleId;
        } catch (ResourceNotFoundException e) {
            log.warn("일정 수정 중 리소스 없음: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "요청하신 일정을 찾을 수 없습니다.");
            return "redirect:/schedule/schedule";
        } catch (Exception e) {
            log.error("일정 수정 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "일정 수정 중 오류가 발생했습니다.");
            return "redirect:/schedule/edit/" + scheduleId;
        }
    }
}
