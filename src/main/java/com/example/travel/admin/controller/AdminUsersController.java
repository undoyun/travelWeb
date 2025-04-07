package com.example.travel.admin.controller;

import com.example.travel.user.model.User;
import com.example.travel.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUsersController {

    private final UserService userService;

    /**
     * 유저 목록 페이지
     * 
     * @param page    페이지 번호
     * @param size    페이지 크기
     * @param role    권한 필터 (관리자, 일반)
     * @param keyword 검색어
     * @param model   Thymeleaf 모델
     * @return users 템플릿
     */
    @GetMapping
    public String getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        // 유저 목록 조회
        Page<User> users = userService.getFilteredUsers(null, role, keyword, pageable);

        // 전체 유저 수
        long totalUsers = userService.countAllUsers();

        model.addAttribute("users", users);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("role", role);
        model.addAttribute("keyword", keyword);

        return "admin/user/adminUsers";
    }

    /**
     * 유저 상세 정보 페이지
     * 
     * @param id    유저 ID
     * @param model Thymeleaf 모델
     * @return user-detail 템플릿
     */
    @GetMapping("/{id}")
    public String getUserDetail(@PathVariable(name = "id") Long id, Model model) {
        try {
            User user = userService.getUserById(id);

            // 유저의 활동 정보 가져오기 (일정, 리뷰, 문의 등)
            int itineraryCount = userService.countUserItineraries(id);
            int reviewCount = userService.countUserReviews(id);
            int inquiryCount = userService.countUserInquiries(id);
            int loginCount = userService.getLoginCount(id);

            // 최근 활동 조회
            List<Map<String, Object>> recentActivities = userService.getRecentActivities(id);

            model.addAttribute("user", user);
            model.addAttribute("itineraryCount", itineraryCount);
            model.addAttribute("reviewCount", reviewCount);
            model.addAttribute("inquiryCount", inquiryCount);
            model.addAttribute("loginCount", loginCount);
            model.addAttribute("recentActivities", recentActivities);

            return "admin/user/adminUser-detail";
        } catch (Exception e) {
            log.error("사용자 상세 정보 조회 중 오류 발생: {}", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    /**
     * 유저 수정 페이지
     * 
     * @param id    유저 ID
     * @param model Thymeleaf 모델
     * @return user-edit 템플릿
     */
    @GetMapping("/{id}/edit")
    public String getEditUserForm(@PathVariable(name = "id") Long id, Model model) {
        try {
            User user = userService.getUserById(id);
            model.addAttribute("user", user);
            return "admin/user/adminUser-edit";
        } catch (Exception e) {
            log.error("사용자 편집 폼 로드 중 오류 발생: {}", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    /**
     * 유저 정보 업데이트
     *
     * @param id                 유저 ID
     * @param updatedUser        수정된 유저 정보
     * @param redirectAttributes 리다이렉트 속성
     * @return 상세 페이지로 리다이렉트
     */
    @PostMapping("/{id}/update")
    public String updateUser(
            @PathVariable(name = "id") Long id,
            @ModelAttribute User updatedUser,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("사용자 업데이트 요청 - ID: {}", id);
            log.info("업데이트할 사용자 정보: {}", updatedUser);

            // 중요 필드 개별 로그
            log.info("업데이트 필드 - 사용자명: {}", updatedUser.getUsername());
            log.info("업데이트 필드 - 이름: {}", updatedUser.getName());
            log.info("업데이트 필드 - 닉네임: {}", updatedUser.getNickname());
            log.info("업데이트 필드 - 이메일: {}", updatedUser.getEmail());
            log.info("업데이트 필드 - 성별: {}", updatedUser.getGender());
            log.info("업데이트 필드 - 생년월일: {}", updatedUser.getBirthdate());
            log.info("업데이트 필드 - 권한: {}", updatedUser.getRole());

            userService.updateUser(id, updatedUser);
            redirectAttributes.addFlashAttribute("successMessage", "사용자 정보가 성공적으로 업데이트되었습니다.");
            return "redirect:/admin/users/" + id;
        } catch (Exception e) {
            log.error("사용자 정보 업데이트 중 오류 발생: {}", e.getMessage(), e);
            log.error("오류 스택 트레이스:", e);
            log.error("오류 종류: {}", e.getClass().getName());

            if (e.getCause() != null) {
                log.error("오류 원인: {}", e.getCause().getMessage());
            }

            redirectAttributes.addFlashAttribute("errorMessage", "사용자 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    /**
     * 유저 삭제
     *
     * @param id                 유저 ID
     * @param redirectAttributes 리다이렉트 속성
     * @return 유저 목록으로 리다이렉트
     */
    @PostMapping("/{id}/delete")
    public String deleteUser(
            @PathVariable(name = "id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "사용자가 성공적으로 삭제되었습니다.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            log.error("사용자 삭제 중 오류 발생: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "사용자 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    /**
     * 유저 정보 API (AJAX용)
     * 
     * @param id 유저 ID
     * @return 유저 정보 JSON
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable(name = "id") Long id) {
        try {
            User user = userService.getUserById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("createdAt", user.getCreatedAt());
            response.put("lastLoginAt", user.getLastLoginAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("사용자 정보 API 요청 중 오류 발생: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}