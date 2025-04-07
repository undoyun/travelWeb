package com.example.travel.counseling.service;

import com.example.travel.counseling.model.Counseling;
import com.example.travel.counseling.model.CounselingCategory;
import com.example.travel.counseling.repository.CounselingRepository;
import com.example.travel.user.model.User;
import com.example.travel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CounselingService {

    private final CounselingRepository counselingRepository;
    private final UserRepository userRepository;

    /**
     * 새로운 문의 생성
     */
    @Transactional
    public Counseling createCounseling(String userId, String title, String content,
            CounselingCategory category) {
        User user;
        try {
            // userId가 숫자인 경우 (ID 기반 조회)
            Long userIdLong = Long.parseLong(userId);
            user = userRepository.findById(userIdLong)
                    .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자가 존재하지 않습니다: " + userId));
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 (username으로 간주하고 조회)
            user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자명의 사용자가 존재하지 않습니다: " + userId));
        }

        Counseling counseling = Counseling.builder()
                .user(user)
                .title(title)
                .content(content)
                .category(category)
                .isAnswered(false)
                .createdAt(LocalDateTime.now())
                .build();

        return counselingRepository.save(counseling);
    }

    /**
     * 문의 상세 조회
     */
    @Transactional(readOnly = true)
    public Optional<Counseling> getCounselingById(Long id) {
        return counselingRepository.findById(id);
    }

    /**
     * 특정 사용자의 문의 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<Counseling> getCounselingsByUserId(String userId, Pageable pageable) {
        try {
            Long userIdLong = Long.parseLong(userId);
            return counselingRepository.findByUserIdOrderByCreatedAtDesc(userIdLong, pageable);
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 username으로 간주하고 해당 사용자의 ID로 조회
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자명의 사용자가 존재하지 않습니다: " + userId));
            return counselingRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        }
    }

    /**
     * 특정 사용자의 특정 카테고리 문의 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<Counseling> getCounselingsByUserIdAndCategory(String userId, CounselingCategory category,
            Pageable pageable) {
        try {
            Long userIdLong = Long.parseLong(userId);
            return counselingRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(userIdLong, category, pageable);
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 username으로 간주하고 해당 사용자의 ID로 조회
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자명의 사용자가 존재하지 않습니다: " + userId));
            return counselingRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(user.getId(), category, pageable);
        }
    }

    /**
     * 특정 사용자의 답변 상태별 문의 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<Counseling> getCounselingsByUserIdAndAnswerStatus(String userId, boolean isAnswered,
            Pageable pageable) {
        try {
            Long userIdLong = Long.parseLong(userId);
            return counselingRepository.findByUserIdAndIsAnsweredOrderByCreatedAtDesc(userIdLong, isAnswered, pageable);
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 username으로 간주하고 해당 사용자의 ID로 조회
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자명의 사용자가 존재하지 않습니다: " + userId));
            return counselingRepository.findByUserIdAndIsAnsweredOrderByCreatedAtDesc(user.getId(), isAnswered,
                    pageable);
        }
    }

    /**
     * 모든 문의 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<Counseling> getAllCounselings(Pageable pageable) {
        return counselingRepository.findAll(pageable);
    }

    /**
     * 답변 여부에 따른 문의 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<Counseling> getCounselingsByAnswerStatus(boolean isAnswered, Pageable pageable) {
        return counselingRepository.findByIsAnsweredOrderByCreatedAtDesc(isAnswered, pageable);
    }

    /**
     * 문의 답변 등록 (관리자용)
     */
    @Transactional
    public Counseling answerCounseling(Long id, String answer, String adminId) {
        Optional<Counseling> optionalCounseling = counselingRepository.findById(id);

        if (optionalCounseling.isPresent()) {
            Counseling counseling = optionalCounseling.get();
            counseling.setAnswer(answer);
            counseling.setAnsweredBy(adminId);
            counseling.setAnsweredAt(LocalDateTime.now());
            counseling.setAnswered(true);

            return counselingRepository.save(counseling);
        } else {
            throw new IllegalArgumentException("해당 ID의 문의가 존재하지 않습니다: " + id);
        }
    }

    /**
     * 미답변 문의 수 조회 (관리자용 대시보드)
     */
    @Transactional(readOnly = true)
    public long getUnansweredCounselingCount() {
        return counselingRepository.countByIsAnswered(false);
    }

    /**
     * 문의 삭제
     */
    @Transactional
    public void deleteCounseling(Long id) {
        counselingRepository.deleteById(id);
    }

    /**
     * 특정 사용자의 문의 삭제
     */
    @Transactional
    public boolean deleteCounselingByIdAndUserId(Long id, String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            Optional<Counseling> counseling = counselingRepository.findByIdAndUserId(id, userIdLong);
            if (counseling.isPresent()) {
                counselingRepository.deleteById(id);
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 username으로 간주하고 해당 사용자의 ID로 조회
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자명의 사용자가 존재하지 않습니다: " + userId));
            Optional<Counseling> counseling = counselingRepository.findByIdAndUserId(id, user.getId());
            if (counseling.isPresent()) {
                counselingRepository.deleteById(id);
                return true;
            }
            return false;
        }
    }

    /**
     * 카테고리별 문의 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<Counseling> getCounselingsByCategory(CounselingCategory category, Pageable pageable) {
        return counselingRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
    }

    /**
     * 최근 문의 목록 조회 (관리자 대시보드용)
     */
    @Transactional(readOnly = true)
    public List<Counseling> getRecentCounselings(int limit) {
        return counselingRepository.findTopByOrderByCreatedAtDesc(limit);
    }

    /**
     * 카테고리별 문의 수 통계 (관리자 대시보드용)
     */
    @Transactional(readOnly = true)
    public long countByCategory(CounselingCategory category) {
        return counselingRepository.countByCategory(category);
    }

    /**
     * 사용자별 문의 수 조회
     */
    @Transactional(readOnly = true)
    public long countByUserId(String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            return counselingRepository.countByUserId(userIdLong);
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 username으로 간주하고 해당 사용자의 ID로 조회
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자명의 사용자가 존재하지 않습니다: " + userId));
            return counselingRepository.countByUserId(user.getId());
        }
    }

    /**
     * 사용자의 미답변 문의 수 조회
     */
    @Transactional(readOnly = true)
    public long countByUserIdAndIsAnswered(String userId, boolean isAnswered) {
        try {
            Long userIdLong = Long.parseLong(userId);
            return counselingRepository.countByUserIdAndIsAnswered(userIdLong, isAnswered);
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 username으로 간주하고 해당 사용자의 ID로 조회
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자명의 사용자가 존재하지 않습니다: " + userId));
            return counselingRepository.countByUserIdAndIsAnswered(user.getId(), isAnswered);
        }
    }

    /**
     * 제목이나 내용으로 검색
     */
    @Transactional(readOnly = true)
    public Page<Counseling> searchCounselingsByUserId(String userId, String keyword, Pageable pageable) {
        try {
            Long userIdLong = Long.parseLong(userId);
            return counselingRepository.findByUserIdAndTitleContainingOrUserIdAndContentContaining(
                    userIdLong, keyword, userIdLong, keyword, pageable);
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 username으로 간주하고 해당 사용자의 ID로 조회
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자명의 사용자가 존재하지 않습니다: " + userId));
            return counselingRepository.findByUserIdAndTitleContainingOrUserIdAndContentContaining(
                    user.getId(), keyword, user.getId(), keyword, pageable);
        }
    }

    /**
     * 특정 사용자의 상태와 카테고리로 문의 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<Counseling> getCounselingsByUserIdAndStatusAndCategory(
            String userId, boolean isAnswered, CounselingCategory category, Pageable pageable) {
        try {
            Long userIdLong = Long.parseLong(userId);
            return counselingRepository.findByUserIdAndIsAnsweredAndCategoryOrderByCreatedAtDesc(
                userIdLong, isAnswered, category, pageable);
        } catch (NumberFormatException e) {
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자명의 사용자가 존재하지 않습니다: " + userId));
            return counselingRepository.findByUserIdAndIsAnsweredAndCategoryOrderByCreatedAtDesc(
                user.getId(), isAnswered, category, pageable);
        }
    }

    /**
     * 문의 수정
     */
    @Transactional
    public Counseling updateCounseling(Long id, String title, String content, CounselingCategory category) {
        Optional<Counseling> optionalCounseling = counselingRepository.findById(id);

        if (optionalCounseling.isPresent()) {
            Counseling counseling = optionalCounseling.get();
            counseling.setTitle(title);
            counseling.setContent(content);
            counseling.setCategory(category);
            counseling.setUpdatedAt(LocalDateTime.now());

            return counselingRepository.save(counseling);
        } else {
            throw new IllegalArgumentException("해당 ID의 문의가 존재하지 않습니다: " + id);
        }
    }
}