package com.example.travel.review.repository;

import com.example.travel.review.model.Review;
import com.example.travel.review.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {

        // 목적지별 리뷰 목록 조회
        Page<Review> findByDestinationContainingOrderByCreatedAtDesc(String destination, Pageable pageable);

        // 별점 순으로 리뷰 목록 조회
        Page<Review> findByDestinationContainingOrderByRatingDescCreatedAtDesc(String destination, Pageable pageable);

        // 댓글 수 기준으로 인기순 정렬
        @Query("SELECT r FROM Review r LEFT JOIN r.comments c WHERE r.destination LIKE %:destination% " +
                        "GROUP BY r.id ORDER BY COUNT(c.id) DESC, r.createdAt DESC")
        Page<Review> findByDestinationOrderByCommentCountDesc(@Param("destination") String destination,
                        Pageable pageable);

        // 사용자별 리뷰 목록 조회
        Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        // 일정별 리뷰 조회
        @Query("SELECT r FROM Review r WHERE r.schedule.id = :scheduleId AND r.user.id = :userId")
        Optional<Review> findByScheduleIdAndUserId(@Param("scheduleId") Long scheduleId, @Param("userId") Long userId);

        // 일정별 리뷰 존재 여부 확인
        @Query("SELECT COUNT(r) > 0 FROM Review r WHERE r.schedule.id = :scheduleId AND r.user.id = :userId")
        boolean existsByScheduleIdAndUserId(@Param("scheduleId") Long scheduleId, @Param("userId") Long userId);

        // 특정 태그가 포함된 리뷰 목록 조회
        @Query("SELECT DISTINCT r FROM Review r JOIN r.tags t WHERE t IN :tags AND r.destination LIKE %:destination%")
        Page<Review> findByTagsAndDestination(@Param("tags") List<String> tags,
                        @Param("destination") String destination, Pageable pageable);

        // 키워드로 리뷰 검색 (제목, 내용 검색)
        @Query("SELECT r FROM Review r WHERE " +
                        "(r.title LIKE %:keyword% OR r.content LIKE %:keyword%) " +
                        "AND (:destination = '' OR r.destination LIKE %:destination%) " +
                        "ORDER BY r.createdAt DESC")
        Page<Review> searchByKeyword(@Param("keyword") String keyword, @Param("destination") String destination,
                        Pageable pageable);

        // 인기 리뷰 상위 조회 (별점과 조회수 기준)
        @Query("SELECT r FROM Review r LEFT JOIN r.comments c " +
                        "GROUP BY r.id " +
                        "ORDER BY r.rating DESC, COUNT(c.id) DESC, r.viewCount DESC, r.createdAt DESC")
        List<Review> findTopReviews(Pageable pageable);

        /**
         * 특정 사용자가 작성한 모든 리뷰 조회
         */
        Page<Review> findByUserId(Long userId, Pageable pageable);

        /**
         * 신고 횟수가 특정 값 이상인 리뷰 조회
         */
        Page<Review> findByReportCountGreaterThan(Integer threshold, Pageable pageable);

        /**
         * 특정 상태의 리뷰 조회
         */
        Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

        /**
         * 키워드를 포함한 리뷰 조회
         */
        @Query("SELECT r FROM Review r WHERE "
                        + "(r.title LIKE %:keyword% OR r.content LIKE %:keyword%) "
                        + "AND r.status = 'APPROVED'")
        Page<Review> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        /**
         * 특정 카테고리의 리뷰 조회
         */
        Page<Review> findByCategory(String category, Pageable pageable);

        /**
         * 추천 리뷰 조회
         */
        Page<Review> findByIsFeaturedTrue(Pageable pageable);

        /**
         * 좋아요가 많은 순서대로 리뷰 조회
         */
        Page<Review> findByStatusOrderByLikesCountDesc(ReviewStatus status, Pageable pageable);

        /**
         * 평점 범위 내의 리뷰 조회
         */
        @Query("SELECT r FROM Review r WHERE r.rating >= :minRating AND r.rating <= :maxRating")
        Page<Review> findByRatingBetween(
                        @Param("minRating") Double minRating,
                        @Param("maxRating") Double maxRating,
                        Pageable pageable);

        /**
         * 가장 최근에 작성된 리뷰 5개를 조회합니다.
         */
        List<Review> findTop5ByOrderByCreatedAtDesc();
}