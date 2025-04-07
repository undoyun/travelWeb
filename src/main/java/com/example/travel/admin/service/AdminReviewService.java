package com.example.travel.admin.service;

import com.example.travel.review.model.Review;
import com.example.travel.review.model.ReviewStatus;
import com.example.travel.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReviewService {

    private final ReviewRepository reviewRepository;

    /**
     * 전체 리뷰 목록 조회 (필터링 포함)
     */
    public Page<Review> getAllReviews(
            String category,
            Double minRating,
            Double maxRating,
            String searchTerm,
            Pageable pageable) {

        if (category != null || minRating != null || maxRating != null || searchTerm != null) {
            return reviewRepository.findWithFilters(category, minRating, maxRating, searchTerm, pageable);
        }

        return reviewRepository.findAll(pageable);
    }

    /**
     * 신고된 리뷰 목록 조회
     */
    public Page<Review> getReportedReviews(Pageable pageable) {
        return reviewRepository.findByReportCountGreaterThan(0, pageable);
    }

    /**
     * 상태별 리뷰 목록 조회
     */
    public Page<Review> getReviewsByStatus(ReviewStatus status, Pageable pageable) {
        return reviewRepository.findByStatus(status, pageable);
    }

    /**
     * 리뷰 상세 조회
     */
    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    /**
     * 리뷰 상태 변경
     */
    @Transactional
    public void updateReviewStatus(Long reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다: " + reviewId));
        review.setStatus(status);
        reviewRepository.save(review);
        log.info("리뷰 {} 상태가 {}로 변경되었습니다", reviewId, status);
    }

    /**
     * 리뷰 신고 횟수 초기화
     */
    @Transactional
    public void clearReportCount(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다: " + reviewId));
        review.clearReports();
        reviewRepository.save(review);
        log.info("리뷰 {} 신고 횟수가 초기화되었습니다", reviewId);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
        log.info("리뷰 {}가 삭제되었습니다", reviewId);
    }
    
    /**
     * 특정 명소 관련 리뷰 목록 조회
     * 현재 리뷰 엔티티에 명소 관련 필드가 없어 비활성화
     */
    /*
    public Page<Review> getReviewsByAttraction(Long attractionId, ReviewStatus status, Pageable pageable) {
        return reviewRepository.findByAttractionIdAndStatus(attractionId, status, pageable);
    }
    
    public List<Review> getApprovedReviewsByAttraction(Long attractionId) {
        return reviewRepository.findByAttractionIdAndStatus(attractionId, ReviewStatus.APPROVED);
    }
    */
}