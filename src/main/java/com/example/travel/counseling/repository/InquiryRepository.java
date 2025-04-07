package com.example.travel.counseling.repository;

import com.example.travel.counseling.model.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 1:1 문의 리포지토리
 */
@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    /**
     * 카테고리 기준 문의 조회
     */
    Page<Inquiry> findByCategory(String category, Pageable pageable);

    /**
     * 답변 상태 기준 문의 조회
     */
    Page<Inquiry> findByIsAnswered(Boolean isAnswered, Pageable pageable);

    /**
     * 카테고리와 답변 상태 기준 문의 조회
     */
    Page<Inquiry> findByCategoryAndIsAnswered(String category, Boolean isAnswered, Pageable pageable);

    /**
     * 특정 사용자의 문의 조회
     */
    Page<Inquiry> findByUserId(Long userId, Pageable pageable);

    /**
     * 미답변 문의 수 조회
     */
    long countByIsAnswered(Boolean isAnswered);
}