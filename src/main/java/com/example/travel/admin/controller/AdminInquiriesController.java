package com.example.travel.admin.controller;

import com.example.travel.admin.dto.InquiryAnswerRequest;
import com.example.travel.admin.dto.InquiryDetailDto;
import com.example.travel.admin.dto.InquiryPageDto;
import com.example.travel.admin.service.InquiryService;
import com.example.travel.counseling.model.CounselingCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminInquiriesController {

    private final InquiryService inquiryService;

    /**
     * 관리자 문의 목록 페이지
     * 
     * @param category (옵션) 문의 카테고리 필터
     * @param answered (옵션) 답변 여부 필터링 (true: 답변 완료, false: 미답변, null: 전체)
     * @param keyword  (옵션) 검색 키워드 (제목 또는 내용)
     * @param page     페이지 번호 (기본: 0)
     * @param model    Thymeleaf에 전달할 모델
     * @return "admin/inquiries" 뷰
     */
    @GetMapping("/admin/inquiries")
    public String getInquiries(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean answered,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        log.info("관리자 문의 목록 조회 - 카테고리: {}, 답변 상태: {}, 키워드: {}, 페이지: {}",
                category, answered, keyword, page);

        // 서비스에서 문의 목록(페이지 단위) 조회
        InquiryPageDto inquiriesPage = inquiryService.getInquiries(category, answered, keyword, page);

        // 전체 미답변 문의 수 조회
        long unansweredCount = inquiryService.countUnansweredInquiries();

        // 모든 카테고리별 문의 수 및 미답변 문의 수 계산
        Map<String, Long> categoryCounts = new HashMap<>();
        Map<String, Long> categoryUnansweredCounts = new HashMap<>();

        // 전체 카테고리 목록
        CounselingCategory[] allCategories = CounselingCategory.values();

        // 각 카테고리별 통계 수집
        for (CounselingCategory cat : allCategories) {
            String displayName = cat.getDisplayName();
            // 각 카테고리별 총 문의 수
            long categoryCount = inquiryService.countInquiriesByCategory(displayName);
            categoryCounts.put(displayName, categoryCount);

            // 각 카테고리별 미답변 문의 수
            long categoryUnansweredCount = inquiryService.countUnansweredInquiriesByCategory(displayName);
            categoryUnansweredCounts.put(displayName, categoryUnansweredCount);
        }

        // 모델에 데이터 추가
        model.addAttribute("inquiriesPage", inquiriesPage);
        model.addAttribute("category", category);
        model.addAttribute("answered", answered);
        model.addAttribute("keyword", keyword);
        model.addAttribute("unansweredCount", unansweredCount);
        model.addAttribute("categories", allCategories);
        model.addAttribute("categoryCounts", categoryCounts);
        model.addAttribute("categoryUnansweredCounts", categoryUnansweredCounts);

        return "admin/inquiries";
    }

    /**
     * 문의 상세 페이지
     * 
     * @param id    문의 ID
     * @param model Thymeleaf에 전달할 모델
     * @return "admin/inquiry-detail" 뷰
     */
    @GetMapping("/admin/inquiries/detail/{id}")
    public String getInquiryDetail(@PathVariable(name = "id") Long id, Model model) {
        try {
            InquiryDetailDto inquiryDetail = inquiryService.getInquiryById(id);
            model.addAttribute("inquiry", inquiryDetail);
            
            // 카테고리 목록 추가
            model.addAttribute("categories", CounselingCategory.values());
            
            // 답변 상태에 따라 답변 폼 표시 여부 설정
            model.addAttribute("showAnswerForm", !inquiryDetail.isAnswered());

            return "admin/inquiry-detail";
        } catch (Exception e) {
            log.error("문의 상세 조회 중 오류 발생: {}", e.getMessage());
            model.addAttribute("error", "문의 상세 조회 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/admin/inquiries";
        }
    }

    /**
     * 문의 답변 처리
     * 
     * @param id                 문의 ID
     * @param answerRequest      답변 내용
     * @param redirectAttributes 리다이렉트 시 전달할 속성
     * @return 문의 상세 페이지로 리다이렉트
     */
    @PostMapping("/admin/inquiries/{id}/answer")
    public String answerInquiry(
            @PathVariable(name = "id") Long id,
            @ModelAttribute InquiryAnswerRequest answerRequest,
            RedirectAttributes redirectAttributes) {

        try {
            // 답변 등록 및 상태 업데이트
            InquiryDetailDto updatedInquiry = inquiryService.answerInquiry(
                    id,
                    answerRequest.getAnswer(),
                    answerRequest.getAdminId());

            log.info("문의 답변 완료: ID={}, 답변자={}", id, answerRequest.getAdminId());
            redirectAttributes.addFlashAttribute("message", "문의가 성공적으로 답변되었습니다.");

            // 사용자에게 알림 설정 (실제 알림 기능이 구현되어 있다면)
            // inquiryService.notifyUserAboutAnswer(id);
        } catch (Exception e) {
            log.error("문의 답변 중 오류 발생: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "문의 답변 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/admin/inquiries/detail/" + id;
    }

    /**
     * 문의 삭제 처리
     * 
     * @param id                 문의 ID
     * @param redirectAttributes 리다이렉트 시 전달할 속성
     * @return 문의 목록 페이지로 리다이렉트
     */
    @PostMapping("/admin/inquiries/{id}/delete")
    public String deleteInquiry(
            @PathVariable(name = "id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            inquiryService.deleteInquiry(id);
            log.info("문의 삭제 완료: ID={}", id);
        } catch (Exception e) {
            log.error("문의 삭제 중 오류 발생: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "문의 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/admin/inquiries";
    }

    /**
     * 문의 답변 상태 업데이트
     *
     * @param id                 문의 ID
     * @param request            상태 업데이트 요청 정보
     * @param redirectAttributes 리다이렉트 시 전달할 속성
     * @return 문의 상세 페이지로 리다이렉트
     */
    @PostMapping("/admin/inquiries/{id}/update-status")
    public String updateInquiryStatus(
            @PathVariable(name = "id") Long id,
            @RequestParam("adminId") String adminId,
            RedirectAttributes redirectAttributes) {

        try {
            inquiryService.updateInquiryStatus(id, adminId);
            redirectAttributes.addFlashAttribute("message", "문의 상태가 성공적으로 업데이트되었습니다.");
            log.info("문의 상태 업데이트 완료: ID={}, 관리자={}", id, adminId);
        } catch (Exception e) {
            log.error("문의 상태 업데이트 중 오류 발생: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "문의 상태 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/admin/inquiries/detail/" + id;
    }
}
