package com.example.travel.admin.controller;

import com.example.travel.admin.service.AdminReviewService;
import com.example.travel.review.model.Review;
import com.example.travel.review.model.ReviewStatus;
import com.example.travel.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;
    private final ReviewRepository reviewRepository;

    /**
     * 리뷰 관리 페이지 - 전체 리뷰 목록 및 필터링
     */
    @GetMapping
    public String reviews(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            Model model) {

        log.info("리뷰 관리 페이지 접속 - 상태: {}, 카테고리: {}, 평점: {} ~ {}, 검색어: {}, 페이지: {}, 크기: {}",
                status, category, minRating, maxRating, searchTerm, page, size);

        try {
            // 정렬 설정 (최신순)
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            // 필터링된 리뷰 목록 조회
            Page<Review> reviewsPage;

            // 신고된 리뷰만 조회
            if ("REPORTED".equals(status)) {
                reviewsPage = adminReviewService.getReportedReviews(pageable);
                log.debug("신고된 리뷰 조회 결과: {} 건", reviewsPage.getTotalElements());
            }
            // 상태별 필터링 (승인됨, 대기중, 거부됨)
            else if (!"ALL".equals(status)) {
                ReviewStatus reviewStatus = ReviewStatus.valueOf(status);
                reviewsPage = adminReviewService.getReviewsByStatus(reviewStatus, pageable);
                log.debug("상태({}) 별 리뷰 조회 결과: {} 건", status, reviewsPage.getTotalElements());
            }
            // 평점별 필터링
            else if (minRating != null && maxRating != null) {
                log.debug("평점 필터 적용: {} ~ {}", minRating, maxRating);
                reviewsPage = reviewRepository.findByRatingBetween(minRating, maxRating, pageable);
                log.debug("평점 필터링 결과: {} 건", reviewsPage.getTotalElements());
            }
            // 전체 조회 (추가 필터 적용)
            else {
                // 모든 리뷰를 가져와서 페이징 처리
                reviewsPage = adminReviewService.getAllReviews(
                        category, minRating, maxRating, searchTerm, pageable);
                log.debug("전체 리뷰 조회 결과: {} 건", reviewsPage.getTotalElements());
            }

            List<Review> reviews = reviewsPage.isEmpty() ? Collections.emptyList() : reviewsPage.getContent();
            log.debug("조회된 리뷰 목록: {}", reviews);

            // 필터 상태 모델에 추가
            model.addAttribute("reviewsPage", reviewsPage);
            model.addAttribute("reviews", reviews);
            model.addAttribute("status", status);
            model.addAttribute("category", category);
            model.addAttribute("minRating", minRating);
            model.addAttribute("maxRating", maxRating);
            model.addAttribute("searchTerm", searchTerm);

            return "admin/adminReviews";
        } catch (Exception e) {
            log.error("리뷰 목록 조회 중 오류 발생", e);
            model.addAttribute("error", "리뷰 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("reviews", Collections.emptyList());
            model.addAttribute("reviewsPage", Page.empty());
            return "admin/adminReviews";
        }
    }

    /**
     * 신고된 리뷰 목록 조회
     */
    @GetMapping("/reported")
    public String reportedReviews(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            Model model) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("reportCount").descending()
                    .and(Sort.by("createdAt").descending()));

            Page<Review> reportedReviewsPage = adminReviewService.getReportedReviews(pageable);
            log.debug("신고된 리뷰 조회 결과: {} 건", reportedReviewsPage.getTotalElements());

            model.addAttribute("reviewsPage", reportedReviewsPage);
            model.addAttribute("reviews", reportedReviewsPage.getContent());
            model.addAttribute("status", "REPORTED");

            return "admin/adminReviews";
        } catch (Exception e) {
            log.error("신고된 리뷰 목록 조회 중 오류 발생", e);
            model.addAttribute("error", "신고된 리뷰 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("reviews", Collections.emptyList());
            model.addAttribute("reviewsPage", Page.empty());
            return "admin/adminReviews";
        }
    }

    /**
     * 리뷰 삭제
     */
    @GetMapping("/delete/{id}")
    public String deleteReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("리뷰 삭제 요청 - ID: {}", id);
        try {
            adminReviewService.deleteReview(id);
            redirectAttributes.addFlashAttribute("message", "리뷰가 성공적으로 삭제되었습니다.");
            log.info("리뷰 삭제 성공 - ID: {}", id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "리뷰 삭제 중 오류가 발생했습니다: " + e.getMessage());
            log.error("리뷰 삭제 중 오류 - ID: {}", id, e);
        }
        return "redirect:/admin/reviews";
    }

    /**
     * 리뷰 상태 변경 (승인, 거부)
     */
    @PostMapping("/{id}/status")
    @ResponseBody
    public String updateReviewStatus(
            @PathVariable Long id,
            @RequestParam ReviewStatus status) {

        log.info("리뷰 {} 상태 변경: {}", id, status);
        try {
            adminReviewService.updateReviewStatus(id, status);
            return "success";
        } catch (Exception e) {
            log.error("리뷰 상태 변경 중 오류 발생 - ID: {}, 상태: {}", id, status, e);
            return "error: " + e.getMessage();
        }
    }

    /**
     * 리뷰 신고 횟수 초기화
     */
    @PostMapping("/{id}/clear-reports")
    @ResponseBody
    public String clearReports(@PathVariable Long id) {
        log.info("리뷰 {} 신고 초기화", id);
        try {
            adminReviewService.clearReportCount(id);
            return "success";
        } catch (Exception e) {
            log.error("리뷰 신고 초기화 중 오류 발생 - ID: {}", id, e);
            return "error: " + e.getMessage();
        }
    }
}