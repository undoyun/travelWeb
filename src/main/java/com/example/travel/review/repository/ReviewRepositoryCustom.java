package com.example.travel.review.repository;

import com.example.travel.review.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 리뷰 고급 검색을 위한 커스텀 리포지토리 인터페이스
 */
public interface ReviewRepositoryCustom {

    /**
     * 다양한 조건으로 리뷰 필터링 검색
     *
     * @param category   리뷰 카테고리
     * @param minRating  최소 평점
     * @param maxRating  최대 평점
     * @param searchTerm 검색어 (제목 또는 내용에 포함)
     * @param pageable   페이지 정보
     * @return 조건에 맞는 리뷰 목록
     */
    Page<Review> findWithFilters(
            String category,
            Double minRating,
            Double maxRating,
            String searchTerm,
            Pageable pageable);
} 