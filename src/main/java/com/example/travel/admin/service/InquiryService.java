package com.example.travel.admin.service;

import com.example.travel.admin.dto.InquiryDetailDto;
import com.example.travel.admin.dto.InquiryDto;
import com.example.travel.admin.dto.InquiryPageDto;
import com.example.travel.counseling.model.Counseling;
import com.example.travel.counseling.model.CounselingCategory;
import com.example.travel.counseling.repository.CounselingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service("adminInquiryService")
@RequiredArgsConstructor
public class InquiryService {

    private final CounselingRepository counselingRepository;

    // 페이지 당 문의 수 (예시)
    private static final int PAGE_SIZE = 10;

    /**
     * 사용자(또는 관리자)가 전달한 카테고리 이름(예: "여행 계획")과 답변 여부를 기반으로
     * 문의 목록을 조회하고 InquiryPageDto로 변환합니다.
     *
     * @param categoryName 문의 카테고리 필터 (예: "여행 계획", "기능 제안", "전체" 등)
     * @param answered     만약 answered가 false이면 미답변 문의만 조회 (true 또는 null이면 전체)
     * @param keyword      검색 키워드 (제목 또는 내용에 포함된 문자열)
     * @param page         요청 페이지 번호 (0-based)
     * @return InquiryPageDto: 현재 페이지 문의 목록, 총 페이지 수, 총 문의 건수 등
     */
    @Transactional(readOnly = true)
    public InquiryPageDto getInquiries(String categoryName, Boolean answered, String keyword, int page) {
        try {
            // 카테고리 이름 "전체" 또는 null인 경우 필터링 없이 전체 조회
            CounselingCategory category = parseCategory(categoryName);

            // 정렬 설정 (최신순)
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

            // 페이지 요청 객체 생성 (0-based)
            PageRequest pageRequest = PageRequest.of(page, PAGE_SIZE, sort);
            Page<Counseling> counselingPage;

            // 검색어가 있는 경우
            boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

            log.info("문의 조회 - 카테고리: {}, 답변 상태: {}, 키워드: {}, 페이지: {}", categoryName, answered, keyword, page);

            if (hasKeyword) {
                // 검색 조건과 함께 조회
                if (category != null && answered != null) {
                    // 카테고리 + 답변 상태 + 키워드 검색
                    counselingPage = counselingRepository
                            .findByCategoryAndIsAnsweredAndTitleContainingOrContentContaining(
                                    category, answered, keyword, keyword, pageRequest);
                } else if (category != null) {
                    // 카테고리 + 키워드 검색
                    counselingPage = counselingRepository.findByCategoryAndTitleContainingOrContentContaining(
                            category, keyword, keyword, pageRequest);
                } else if (answered != null) {
                    // 답변 상태 + 키워드 검색
                    counselingPage = counselingRepository.findByIsAnsweredAndTitleContainingOrContentContaining(
                            answered, keyword, keyword, pageRequest);
                } else {
                    // 키워드만 검색
                    counselingPage = counselingRepository.findByTitleContainingOrContentContaining(
                            keyword, keyword, pageRequest);
                }
            } else {
                // 키워드 없이 조회
                if (category != null && answered != null) {
                    // 카테고리 + 답변 상태 필터
                    counselingPage = counselingRepository.findByCategoryAndIsAnswered(category, answered, pageRequest);
                } else if (category != null) {
                    // 카테고리 필터
                    counselingPage = counselingRepository.findByCategoryOrderByCreatedAtDesc(category, pageRequest);
                } else if (answered != null) {
                    // 답변 상태 필터
                    counselingPage = counselingRepository.findByIsAnsweredOrderByCreatedAtDesc(answered, pageRequest);
                } else {
                    // 필터 없음, 전체 조회
                    counselingPage = counselingRepository.findAll(pageRequest);
                }
            }

            log.info("조회된 결과 수: {}", counselingPage.getContent().size());

            // InquiryDto로 변환
            List<InquiryDto> inquiryDtos = counselingPage.getContent().stream()
                    .map(counseling -> InquiryDto.builder()
                            .id(counseling.getId())
                            .title(counseling.getTitle())
                            .category(counseling.getCategory().getDisplayName()) // displayName 사용
                            .writer(counseling.getUser() != null ? counseling.getUser().getUsername() : "알 수 없음")
                            .createdAt(counseling.getCreatedAt())
                            .isAnswered(counseling.isAnswered())
                            .build())
                    .collect(Collectors.toList());

            return InquiryPageDto.builder()
                    .content(inquiryDtos)
                    .totalPages(counselingPage.getTotalPages())
                    .totalElements(counselingPage.getTotalElements())
                    .currentPage(counselingPage.getNumber())
                    .build();
        } catch (Exception e) {
            log.error("문의 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            // 오류 발생 시 빈 목록 반환
            return InquiryPageDto.builder()
                    .content(List.of())
                    .totalPages(0)
                    .totalElements(0)
                    .currentPage(0)
                    .build();
        }
    }

    /**
     * 미답변 문의 수를 조회합니다.
     * 
     * @return 미답변 문의 수
     */
    @Transactional(readOnly = true)
    public long countUnansweredInquiries() {
        return counselingRepository.countByIsAnswered(false);
    }

    /**
     * 카테고리별 미답변 문의 수를 조회합니다.
     * 
     * @param categoryName 카테고리 이름
     * @return 해당 카테고리의 미답변 문의 수
     */
    @Transactional(readOnly = true)
    public long countUnansweredInquiriesByCategory(String categoryName) {
        try {
            CounselingCategory category = parseCategory(categoryName);
            if (category == null) {
                // 전체 카테고리의 미답변 문의 수
                return counselingRepository.countByIsAnswered(false);
            }
            // 특정 카테고리의 미답변 문의 수
            return counselingRepository.countByCategoryAndIsAnswered(category, false);
        } catch (Exception e) {
            log.error("미답변 문의 수 조회 중 오류 발생: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * ID로 문의 상세 정보를 조회합니다.
     *
     * @param id 문의 ID
     * @return InquiryDetailDto 문의 상세 정보
     * @throws NoSuchElementException 해당 ID의 문의가 없는 경우
     */
    @Transactional(readOnly = true)
    public InquiryDetailDto getInquiryById(Long id) {
        Counseling counseling = counselingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ID가 " + id + "인 문의를 찾을 수 없습니다."));

        return InquiryDetailDto.fromEntity(counseling);
    }

    /**
     * 문의에 답변을 등록합니다.
     *
     * @param id      문의 ID
     * @param answer  답변 내용
     * @param adminId 답변 등록자 ID
     * @return 업데이트된 InquiryDetailDto
     * @throws NoSuchElementException 해당 ID의 문의가 없는 경우
     */
    @Transactional
    public InquiryDetailDto answerInquiry(Long id, String answer, String adminId) {
        try {
            Counseling counseling = counselingRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("ID가 " + id + "인 문의를 찾을 수 없습니다."));

            counseling.setAnswer(answer);
            counseling.setAnsweredBy(adminId);
            counseling.setAnsweredAt(LocalDateTime.now());
            counseling.setAnswered(true);

            Counseling savedCounseling = counselingRepository.save(counseling);

            log.info("문의 답변 등록 완료 - ID: {}, 관리자: {}", id, adminId);

            return InquiryDetailDto.fromEntity(savedCounseling);
        } catch (Exception e) {
            log.error("문의 답변 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 문의를 삭제합니다.
     * 
     * @param id 문의 ID
     */
    @Transactional
    public void deleteInquiry(Long id) {
        try {
            counselingRepository.deleteById(id);
            log.info("문의 삭제 완료 - ID: {}", id);
        } catch (Exception e) {
            log.error("문의 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 문의의 답변 상태를 업데이트합니다.
     *
     * @param id      문의 ID
     * @param adminId 관리자 ID
     * @return 업데이트된 InquiryDetailDto
     * @throws NoSuchElementException 해당 ID의 문의가 없는 경우
     */
    @Transactional
    public InquiryDetailDto updateInquiryStatus(Long id, String adminId) {
        try {
            Counseling counseling = counselingRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("ID가 " + id + "인 문의를 찾을 수 없습니다."));

            // 이미 답변이 있는 경우에만 상태 업데이트
            if (counseling.getAnswer() != null && !counseling.getAnswer().isEmpty()) {
                counseling.setAnswered(true);
                counseling.setAnsweredAt(LocalDateTime.now());
                counseling.setAnsweredBy(adminId);

                Counseling savedCounseling = counselingRepository.save(counseling);
                log.info("문의 상태 업데이트 완료 - ID: {}, 관리자: {}", id, adminId);

                return InquiryDetailDto.fromEntity(savedCounseling);
            } else {
                throw new IllegalStateException("답변이 없는 문의의 상태는 변경할 수 없습니다.");
            }
        } catch (Exception e) {
            log.error("문의 상태 업데이트 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 문자열(예: "여행 계획")를 CounselingCategory Enum으로 변환합니다.
     * "전체" 또는 null이 전달되면 null을 반환하여 전체 조회로 처리합니다.
     *
     * @param categoryName 사용자가 입력한 혹은 선택한 카테고리 이름 (디스플레이용)
     * @return 해당하는 CounselingCategory Enum 상수, 또는 null (전체 조회)
     * @throws IllegalArgumentException 지원하지 않는 카테고리인 경우
     */
    private CounselingCategory parseCategory(String categoryName) {
        if (categoryName == null || categoryName.isEmpty() || "전체".equals(categoryName)) {
            return null;
        }

        try {
            // 여기에서는 카테고리 이름을 CounselingCategory 열거형으로 변환하는 로직을 구현합니다.
            // 디스플레이 이름으로 Enum 찾기
            for (CounselingCategory category : CounselingCategory.values()) {
                if (category.getDisplayName().equalsIgnoreCase(categoryName)) {
                    return category;
                }
            }

            // 위에서 찾지 못했다면 직접 Enum 이름으로 변환 시도
            return CounselingCategory.valueOf(categoryName.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 카테고리 이름: {}", categoryName);
            return null;
        }
    }

    /**
     * 카테고리별 문의 수를 조회합니다.
     * 
     * @param categoryName 카테고리 이름
     * @return 해당 카테고리의 문의 수
     */
    @Transactional(readOnly = true)
    public long countInquiriesByCategory(String categoryName) {
        try {
            CounselingCategory category = parseCategory(categoryName);
            if (category == null) {
                // 전체 카테고리의 문의 수
                return counselingRepository.count();
            }
            // 특정 카테고리의 문의 수
            return counselingRepository.countByCategory(category);
        } catch (Exception e) {
            log.error("카테고리별 문의 수 조회 중 오류 발생: {}", e.getMessage(), e);
            return 0;
        }
    }
}
