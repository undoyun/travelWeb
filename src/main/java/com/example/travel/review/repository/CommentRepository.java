package com.example.travel.review.repository;

import com.example.travel.review.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 리뷰별 댓글 목록 조회
    List<Comment> findByReviewIdOrderByCreatedAtAsc(Long reviewId);

    // 리뷰별 댓글 목록 페이징 조회
    Page<Comment> findByReviewIdOrderByCreatedAtAsc(Long reviewId, Pageable pageable);

    // 사용자별 댓글 목록 조회
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 리뷰의 댓글 수 조회
    long countByReviewId(Long reviewId);

    // 특정 사용자가 작성한 댓글 삭제
    void deleteByReviewIdAndUserId(Long reviewId, Long userId);
}