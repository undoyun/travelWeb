package com.example.travel.counseling.controller;

import com.example.travel.counseling.model.Counseling;
import com.example.travel.counseling.model.CounselingCategory;
import com.example.travel.counseling.service.CounselingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/counseling")
public class CounselingController {

    private final CounselingService counselingService;

    /**
     * 사용자 문의 목록 페이지
     */
    @GetMapping
    public String counselingList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication,
            Model model) {

        String userId = authentication.getName();
        Page<Counseling> counselings;

        // 검색어가 있는 경우
        if (keyword != null && !keyword.trim().isEmpty()) {
            counselings = counselingService.searchCounselingsByUserId(userId, keyword, pageable);
        }
        // 상태와 카테고리 필터 적용
        else {
            boolean hasStatusFilter = status != null;

            boolean hasCategoryFilter = category != null;
            // 상태 필터 변환
            Boolean isAnswered = hasStatusFilter ? Boolean.parseBoolean(status) : null;
            // 카테고리 필터 변환
            CounselingCategory counselingCategory = null;
            if (hasCategoryFilter) {
                try {
                    counselingCategory = CounselingCategory.valueOf(category);
                } catch (IllegalArgumentException e) {
                    // 잘못된 카테고리값이 들어온 경우 무시
                }
            }

            // 필터 조합에 따른 조회
            if (hasStatusFilter && hasCategoryFilter && counselingCategory != null) {
                // 상태와 카테고리 모두 적용
                counselings = counselingService.getCounselingsByUserIdAndStatusAndCategory(
                        userId, isAnswered, counselingCategory, pageable);
            } else if (hasStatusFilter) {
                // 상태만 적용
                counselings = counselingService.getCounselingsByUserIdAndAnswerStatus(
                        userId, isAnswered, pageable);
            } else if (hasCategoryFilter && counselingCategory != null) {
                // 카테고리만 적용
                counselings = counselingService.getCounselingsByUserIdAndCategory(
                        userId, counselingCategory, pageable);
            } else {
                // 필터 없음
                counselings = counselingService.getCounselingsByUserId(userId, pageable);
            }
        }

        // 사용자 문의 통계 추가
        long totalCounselings = counselingService.countByUserId(userId);
        long unansweredCounselings = counselingService.countByUserIdAndIsAnswered(userId, false);

        model.addAttribute("counselings", counselings);
        model.addAttribute("categories", CounselingCategory.values());
        model.addAttribute("totalCounselings", totalCounselings);
        model.addAttribute("unansweredCounselings", unansweredCounselings);

        // 현재 필터 상태 추가
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentCategory", category);
        model.addAttribute("currentKeyword", keyword);

        return "counseling/counseling";
    }

    /**
     * 문의 작성 페이지
     */
    @GetMapping("/create")
    public String createCounselingForm(Model model) {
        model.addAttribute("categories", CounselingCategory.values());
        return "counseling/counselingCreate";
    }

    /**
     * 문의 작성 처리
     */
    @PostMapping("/create")
    public String createCounseling(
            @RequestParam(name = "title") String title,
            @RequestParam(name = "content") String content,
            @RequestParam(name = "category") CounselingCategory category,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String userId = authentication.getName();

        counselingService.createCounseling(userId, title, content, category);

        redirectAttributes.addFlashAttribute("message", "문의가 성공적으로 등록되었습니다.");
        return "redirect:/counseling";
    }

    /**
     * 문의 상세 페이지
     */
    @GetMapping("/{id}")
    public String counselingDetail(@PathVariable(name = "id") Long id, Authentication authentication, Model model) {
        String userId = authentication.getName();
        Optional<Counseling> optionalCounseling = counselingService.getCounselingById(id);

        if (optionalCounseling.isPresent()) {
            Counseling counseling = optionalCounseling.get();

            // 본인 문의가 아니고, 관리자도 아닌 경우 접근 제한
            if ((counseling.getUser() == null || !counseling.getUser().getUsername().equals(userId))
                    && !authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/counseling?error=unauthorized";
            }

            model.addAttribute("counseling", counseling);
            return "counseling/counselingDetails";
        } else {
            return "redirect:/counseling?error=not_found";
        }
    }

    // 기존 경로도 유지하기 위한 추가 메서드
    @GetMapping("/counselingDetails/{id}")
    public String counselingDetails(@PathVariable(name = "id") Long id, Authentication authentication, Model model) {
        return counselingDetail(id, authentication, model);
    }

    /**
     * 문의 삭제 처리
     */
    @PostMapping("/{id}/delete")
    public String deleteCounseling(
            @PathVariable(name = "id") Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String userId = authentication.getName();
        Optional<Counseling> optionalCounseling = counselingService.getCounselingById(id);

        if (optionalCounseling.isPresent()) {
            Counseling counseling = optionalCounseling.get();

            // 본인 문의이거나 관리자인 경우만 삭제 가능
            if ((counseling.getUser() != null && counseling.getUser().getUsername().equals(userId))
                    || authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {

                counselingService.deleteCounseling(id);
                redirectAttributes.addFlashAttribute("message", "문의가 성공적으로 삭제되었습니다.");
                return "redirect:/counseling";
            } else {
                return "redirect:/counseling?error=unauthorized";
            }
        } else {
            return "redirect:/counseling?error=not_found";
        }
    }

    // 기존 경로도 유지
    @PostMapping("/counselingDelete/{id}")
    public String counselingDelete(
            @PathVariable(name = "id") Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return deleteCounseling(id, authentication, redirectAttributes);
    }

    /**
     * 카테고리별 문의 목록 조회
     */
    @GetMapping("/category/{category}")
    public String getCounselingsByCategory(
            @PathVariable(name = "category") CounselingCategory category,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication,
            Model model) {

        String userId = authentication.getName();
        Page<Counseling> counselings = counselingService.getCounselingsByUserIdAndCategory(userId, category, pageable);

        model.addAttribute("counselings", counselings);
        model.addAttribute("categories", CounselingCategory.values());
        model.addAttribute("currentCategory", category);

        return "counseling/counseling";
    }

    /**
     * 답변 상태별 문의 목록 조회
     */
    @GetMapping("/status/{answered}")
    public String getCounselingsByAnswerStatus(
            @PathVariable(name = "answered") boolean isAnswered,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication,
            Model model) {

        String userId = authentication.getName();
        Page<Counseling> counselings = counselingService.getCounselingsByUserIdAndAnswerStatus(userId, isAnswered,
                pageable);

        model.addAttribute("counselings", counselings);
        model.addAttribute("categories", CounselingCategory.values());
        model.addAttribute("currentStatus", isAnswered);

        return "counseling/counseling";
    }

    /**
     * 문의 FAQ 페이지
     */
    @GetMapping("/faq")
    public String faqPage() {
        return "counseling/faq";
    }

    /**
     * 문의 검색 (AJAX)
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchCounselings(
            @RequestParam(name = "keyword") String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        String userId = authentication.getName();
        Page<Counseling> counselings = counselingService.searchCounselingsByUserId(userId, keyword, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("counselings", counselings.getContent());
        response.put("totalPages", counselings.getTotalPages());
        response.put("currentPage", counselings.getNumber());

        return ResponseEntity.ok(response);
    }

    /**
     * 문의 수정 페이지
     */
    @GetMapping("/edit/{id}")
    public String editCounselingForm(@PathVariable(name = "id") Long id, Authentication authentication, Model model) {
        String userId = authentication.getName();
        Optional<Counseling> optionalCounseling = counselingService.getCounselingById(id);

        if (optionalCounseling.isPresent()) {
            Counseling counseling = optionalCounseling.get();

            // 본인 문의가 아니면 목록으로 리다이렉트
            if (counseling.getUser() == null || !counseling.getUser().getUsername().equals(userId)) {
                return "redirect:/counseling?error=unauthorized";
            }

            // 이미 답변이 달린 문의는 수정 불가
            if (counseling.isAnswered()) {
                return "redirect:/counseling/" + id + "?error=already_answered";
            }

            model.addAttribute("counseling", counseling);
            model.addAttribute("categories", CounselingCategory.values());
            return "counseling/counselingEdit";
        } else {
            return "redirect:/counseling?error=not_found";
        }
    }

    // 기존 경로도 유지
    @GetMapping("/counselingEdit/{id}")
    public String counselingEdit(@PathVariable(name = "id") Long id, Authentication authentication, Model model) {
        return editCounselingForm(id, authentication, model);
    }

    /**
     * 문의 수정 처리
     */
    @PostMapping("/edit/{id}")
    public String updateCounseling(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "title") String title,
            @RequestParam(name = "content") String content,
            @RequestParam(name = "category") CounselingCategory category,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String userId = authentication.getName();
        Optional<Counseling> optionalCounseling = counselingService.getCounselingById(id);

        if (optionalCounseling.isPresent()) {
            Counseling counseling = optionalCounseling.get();

            // 본인 문의가 아니면 목록으로 리다이렉트
            if (counseling.getUser() == null || !counseling.getUser().getUsername().equals(userId)) {
                return "redirect:/counseling?error=unauthorized";
            }

            // 이미 답변이 달린 문의는 수정 불가
            if (counseling.isAnswered()) {
                return "redirect:/counseling/" + id + "?error=already_answered";
            }

            counselingService.updateCounseling(id, title, content, category);
            redirectAttributes.addFlashAttribute("message", "문의가 성공적으로 수정되었습니다.");
            return "redirect:/counseling/" + id;
        } else {
            return "redirect:/counseling?error=not_found";
        }
    }
    
    // 기존 경로도 유지
    @PostMapping("/counselingEdit/{id}")
    public String counselingUpdate(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "title") String title,
            @RequestParam(name = "content") String content,
            @RequestParam(name = "category") CounselingCategory category,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        return updateCounseling(id, title, content, category, authentication, redirectAttributes);
    }
}