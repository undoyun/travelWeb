package com.example.travel.counseling.repository;

import com.example.travel.counseling.model.Counseling;
import com.example.travel.counseling.model.CounselingCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounselingRepository extends JpaRepository<Counseling, Long> {

        // 특정 사용자의 모든 문의 조회 (페이징)
        @Query("SELECT c FROM Counseling c WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
        Page<Counseling> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

        // 특정 사용자의 모든 문의 조회 (리스트)
        @Query("SELECT c FROM Counseling c WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
        List<Counseling> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

        // 특정 ID와 사용자 ID로 문의 조회
        @Query("SELECT c FROM Counseling c WHERE c.id = :id AND c.user.id = :userId")
        Optional<Counseling> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

        // 답변 여부에 따른 문의 조회
        Page<Counseling> findByIsAnsweredOrderByCreatedAtDesc(boolean isAnswered, Pageable pageable);

        // 특정 사용자의 답변 여부에 따른 문의 조회
        @Query("SELECT c FROM Counseling c WHERE c.user.id = :userId AND c.isAnswered = :isAnswered ORDER BY c.createdAt DESC")
        Page<Counseling> findByUserIdAndIsAnsweredOrderByCreatedAtDesc(
                        @Param("userId") Long userId, @Param("isAnswered") boolean isAnswered, Pageable pageable);

        // 카테고리별 문의 조회
        Page<Counseling> findByCategoryOrderByCreatedAtDesc(CounselingCategory category, Pageable pageable);

        // 카테고리와 답변 여부에 따른 문의 조회
        Page<Counseling> findByCategoryAndIsAnswered(CounselingCategory category, boolean isAnswered,
                        Pageable pageable);

        // 특정 사용자의 카테고리별 문의 조회
        @Query("SELECT c FROM Counseling c WHERE c.user.id = :userId AND c.category = :category ORDER BY c.createdAt DESC")
        Page<Counseling> findByUserIdAndCategoryOrderByCreatedAtDesc(
                        @Param("userId") Long userId, @Param("category") CounselingCategory category,
                        Pageable pageable);

        // 미답변 문의 수 조회
        long countByIsAnswered(boolean isAnswered);

        // 카테고리별 문의 수 조회
        long countByCategory(CounselingCategory category);

        // 카테고리와 답변 상태별 문의 수 조회
        long countByCategoryAndIsAnswered(CounselingCategory category, boolean isAnswered);

        // 사용자별 문의 수 조회
        @Query("SELECT COUNT(c) FROM Counseling c WHERE c.user.id = :userId")
        long countByUserId(@Param("userId") Long userId);

        // 사용자의 미답변 문의 수 조회
        @Query("SELECT COUNT(c) FROM Counseling c WHERE c.user.id = :userId AND c.isAnswered = :isAnswered")
        long countByUserIdAndIsAnswered(@Param("userId") Long userId, @Param("isAnswered") boolean isAnswered);

        // 최근 문의 목록 조회
        @Query(value = "SELECT c FROM Counseling c ORDER BY c.createdAt DESC")
        List<Counseling> findTopByOrderByCreatedAtDesc(@Param("limit") int limit);

        // 월별 문의 통계
        @Query(value = "SELECT MONTH(created_at) as month, COUNT(*) as count FROM counselings " +
                        "WHERE YEAR(created_at) = :year GROUP BY MONTH(created_at)", nativeQuery = true)
        List<Object[]> getMonthlyStatsByYear(@Param("year") int year);

        // 제목이나 내용으로 검색 (특정 사용자)
        @Query("SELECT c FROM Counseling c WHERE c.user.id = :userId1 AND (c.title LIKE %:title% OR " +
                        "c.user.id = :userId2 AND c.content LIKE %:content%)")
        Page<Counseling> findByUserIdAndTitleContainingOrUserIdAndContentContaining(
                        @Param("userId1") Long userId1, @Param("title") String title,
                        @Param("userId2") Long userId2, @Param("content") String content, Pageable pageable);

        // 전체 키워드 검색 (관리자용)
        @Query("SELECT c FROM Counseling c WHERE c.title LIKE %:keyword% OR c.content LIKE %:keyword%")
        Page<Counseling> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        // 제목 또는 내용으로 검색
        @Query("SELECT c FROM Counseling c WHERE c.title LIKE %:title% OR c.content LIKE %:content%")
        Page<Counseling> findByTitleContainingOrContentContaining(
                        @Param("title") String title,
                        @Param("content") String content,
                        Pageable pageable);

        // 카테고리, 답변 상태, 제목 또는 내용으로 검색
        @Query("SELECT c FROM Counseling c WHERE c.category = :category AND c.isAnswered = :isAnswered AND (c.title LIKE %:title% OR c.content LIKE %:content%)")
        Page<Counseling> findByCategoryAndIsAnsweredAndTitleContainingOrContentContaining(
                        @Param("category") CounselingCategory category,
                        @Param("isAnswered") boolean isAnswered,
                        @Param("title") String title,
                        @Param("content") String content,
                        Pageable pageable);

        // 카테고리, 제목 또는 내용으로 검색
        @Query("SELECT c FROM Counseling c WHERE c.category = :category AND (c.title LIKE %:title% OR c.content LIKE %:content%)")
        Page<Counseling> findByCategoryAndTitleContainingOrContentContaining(
                        @Param("category") CounselingCategory category,
                        @Param("title") String title,
                        @Param("content") String content,
                        Pageable pageable);

        // 답변 상태, 제목 또는 내용으로 검색
        @Query("SELECT c FROM Counseling c WHERE c.isAnswered = :isAnswered AND (c.title LIKE %:title% OR c.content LIKE %:content%)")
        Page<Counseling> findByIsAnsweredAndTitleContainingOrContentContaining(
                        @Param("isAnswered") boolean isAnswered,
                        @Param("title") String title,
                        @Param("content") String content,
                        Pageable pageable);

        // 특정 사용자의 상태와 카테고리별 문의 조회
        @Query("SELECT c FROM Counseling c WHERE c.user.id = :userId AND c.isAnswered = :isAnswered AND c.category = :category ORDER BY c.createdAt DESC")
        Page<Counseling> findByUserIdAndIsAnsweredAndCategoryOrderByCreatedAtDesc(
                        @Param("userId") Long userId,
                        @Param("isAnswered") boolean isAnswered,
                        @Param("category") CounselingCategory category,
                        Pageable pageable);
}