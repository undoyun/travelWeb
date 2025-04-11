package com.example.travel.counseling.service;

import com.example.travel.counseling.model.Inquiry;
import com.example.travel.counseling.repository.InquiryRepository;
import com.example.travel.user.model.User;
import com.example.travel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 1:1 문의 서비스
 */
@Slf4j
@Service("counselingInquiryService")
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    /**
     * 새 문의 등록
     */
    @Transactional
    public Inquiry createInquiry(Inquiry inquiry) {
        return inquiryRepository.save(inquiry);
    }

    /**
     * 문의 상세 조회
     */
    @Transactional(readOnly = true)
    public Inquiry getInquiryById(Long id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다: " + id));
    }

    /**
     * 모든 문의 조회
     */
    @Transactional(readOnly = true)
    public Page<Inquiry> getAllInquiries(Pageable pageable) {
        return inquiryRepository.findAll(pageable);
    }

    /**
     * 카테고리별 문의 조회
     */
    @Transactional(readOnly = true)
    public Page<Inquiry> getInquiriesByCategory(String category, Pageable pageable) {
        return inquiryRepository.findByCategory(category, pageable);
    }

    /**
     * 답변 상태별 문의 조회
     */
    @Transactional(readOnly = true)
    public Page<Inquiry> getInquiriesByAnswered(Boolean isAnswered, Pageable pageable) {
        return inquiryRepository.findByIsAnswered(isAnswered, pageable);
    }

    /**
     * 카테고리 및 답변 상태별 문의 조회
     */
    @Transactional(readOnly = true)
    public Page<Inquiry> getInquiriesByCategoryAndAnswered(String category, Boolean isAnswered, Pageable pageable) {
        return inquiryRepository.findByCategoryAndIsAnswered(category, isAnswered, pageable);
    }

    /**
     * 특정 사용자의 문의 조회
     */
    @Transactional(readOnly = true)
    public Page<Inquiry> getInquiriesByUserId(Long userId, Pageable pageable) {
        return inquiryRepository.findByUserId(userId, pageable);
    }

    /**
     * 문의 답변 등록/수정
     */
    @Transactional
    public Inquiry answerInquiry(Long inquiryId, String answer) {
        Inquiry inquiry = getInquiryById(inquiryId);

        // 현재 인증된 관리자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String adminUsername = authentication.getName();
            User admin = userRepository.findByUsername(adminUsername)
                    .orElseThrow(() -> new IllegalStateException("관리자 정보를 찾을 수 없습니다"));

            inquiry.answer(answer, admin);
            return inquiryRepository.save(inquiry);
        } else {
            throw new IllegalStateException("관리자 인증 정보가 유효하지 않습니다");
        }
    }

    /**
     * 문의 삭제
     */
    @Transactional
    public void deleteInquiry(Long id) {
        inquiryRepository.deleteById(id);
    }

    /**
     * 미답변 문의 수 조회
     */
    @Transactional(readOnly = true)
    public long countUnansweredInquiries() {
        return inquiryRepository.countByIsAnswered(false);
    }

    /**
     * 전체 문의 수를 조회합니다.
     * 
     * @return 전체 문의 수
     */
    public long getTotalInquiriesCount() {
        return inquiryRepository.count();
    }

    /**
     * 미답변 문의 개수를 반환합니다.
     * 
     * @return 미답변 문의 개수
     */
    public long getUnansweredInquiriesCount() {
        return inquiryRepository.countByIsAnswered(false);
    }
}