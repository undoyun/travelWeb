package com.example.travel.review.service;

import com.example.travel.exception.ResourceNotFoundException;
import com.example.travel.exception.UnauthorizedAccessException;
import com.example.travel.review.dto.CommentDTO;
import com.example.travel.review.dto.ReviewDTO;
import com.example.travel.review.model.Review;
import com.example.travel.review.model.ReviewStatus;
import com.example.travel.review.repository.ReviewRepository;
import com.example.travel.schedule.model.Schedule;
import com.example.travel.user.model.User;
import com.example.travel.user.service.UserService;
import com.example.travel.admin.dto.ReviewSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Value("${app.upload.review-images:uploads/reviews}")
    private String reviewImagesDir;

    // 특정 일정에 대한 리뷰 작성 여부 확인
    @Transactional(readOnly = true)
    public boolean hasReviewForSchedule(Long scheduleId, String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            return reviewRepository.existsByScheduleIdAndUserId(scheduleId, userIdLong);
        } catch (NumberFormatException e) {
            log.error("Error parsing userId: {} to Long", userId, e);
            return false;
        }
    }

    // 리뷰 저장
    @Transactional
    public ReviewDTO saveReview(ReviewDTO reviewDTO, List<MultipartFile> images, Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);

        // 이미 해당 일정에 대한 리뷰를 작성했는지 확인
        if (reviewRepository.existsByScheduleIdAndUserId(reviewDTO.getScheduleId(), user.getId())) {
            throw new IllegalStateException("이미 이 여행 일정에 대한 리뷰를 작성하셨습니다.");
        }

        Review review = Review.builder()
                .title(reviewDTO.getTitle())
                .content(reviewDTO.getContent())
                .rating(reviewDTO.getRating().doubleValue())
                .destination(reviewDTO.getDestination())
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Schedule 참조 설정
        if (reviewDTO.getScheduleId() != null) {
            Schedule schedule = new Schedule();
            schedule.setId(reviewDTO.getScheduleId());
            review.setSchedule(schedule);
        }

        // 태그 처리
        if (reviewDTO.getTags() != null && !reviewDTO.getTags().isEmpty()) {
            reviewDTO.getTags().forEach(review::addTag);
        }

        // 이미지 저장
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String imageUrl = fileStorageService.storeFile(image, reviewImagesDir);
                    review.addImage(imageUrl);
                }
            }
        }

        Review savedReview = reviewRepository.save(review);
        return convertToDTO(savedReview, user.getId());
    }

    // 리뷰 수정
    @Transactional
    public ReviewDTO updateReview(Long reviewId, ReviewDTO reviewDTO, List<MultipartFile> newImages,
            Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        Review review = findReviewById(reviewId);

        // 작성자 확인
        if (!review.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        review.setTitle(reviewDTO.getTitle());
        review.setContent(reviewDTO.getContent());
        review.setRating(reviewDTO.getRating().doubleValue());

        // 기존 태그 제거 후 새로 추가
        review.getTags().clear();
        if (reviewDTO.getTags() != null && !reviewDTO.getTags().isEmpty()) {
            reviewDTO.getTags().forEach(review::addTag);
        }

        // 새 이미지가 있다면 추가
        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile image : newImages) {
                if (!image.isEmpty()) {
                    String imageUrl = fileStorageService.storeFile(image, reviewImagesDir);
                    review.addImage(imageUrl);
                }
            }
        }

        Review updatedReview = reviewRepository.save(review);
        return convertToDTO(updatedReview, user.getId());
    }

    // 일반 사용자용 리뷰 삭제 메서드
    @Transactional
    public void deleteReviewByUser(Long reviewId, Authentication authentication) {
        Review review = findReviewById(reviewId);

        // 작성자 또는 관리자 확인
        if (isAdminOrAuthor(authentication, review)) {
            // 이미지 파일 삭제
            for (String imageUrl : review.getImages()) {
                try {
                    fileStorageService.deleteFile(imageUrl);
                } catch (Exception e) {
                    log.warn("이미지 파일 삭제 실패: {}", imageUrl);
                }
            }

            reviewRepository.delete(review);
            log.info("리뷰 ID {} 삭제됨 (요청자: {})", reviewId, authentication.getName());
        } else {
            throw new UnauthorizedAccessException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }
    }

    // 관리자용 리뷰 하드 삭제 메서드 (DB에서 완전히 제거)
    @Transactional
    public void hardDeleteReview(Long id) {
        Review review = findReviewById(id);

        // 이미지 파일 삭제
        for (String imageUrl : review.getImages()) {
            try {
                fileStorageService.deleteFile(imageUrl);
            } catch (Exception e) {
                log.warn("이미지 파일 삭제 실패: {}", imageUrl);
            }
        }

        reviewRepository.delete(review);
        log.info("관리자에 의해 리뷰 ID {} 완전히 삭제됨", id);
    }

    // 목적지별 리뷰 목록 조회 (최신순)
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsByDestination(String destination, Pageable pageable,
            Authentication authentication) {
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.getUserFromAuthentication(authentication);
            userId = user.getId();
        }

        Page<Review> reviews = reviewRepository.findByDestinationContainingOrderByCreatedAtDesc(destination, pageable);
        Long finalUserId = userId;
        return reviews.map(review -> convertToDTO(review, finalUserId));
    }

    // 목적지별 리뷰 목록 조회 (별점순)
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsByDestinationOrderByRating(String destination, Pageable pageable,
            Authentication authentication) {
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.getUserFromAuthentication(authentication);
            userId = user.getId();
        }

        Page<Review> reviews = reviewRepository.findByDestinationContainingOrderByRatingDescCreatedAtDesc(destination,
                pageable);
        Long finalUserId = userId;
        return reviews.map(review -> convertToDTO(review, finalUserId));
    }

    // 목적지별 리뷰 목록 조회 (인기순)
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsByDestinationOrderByPopularity(String destination, Pageable pageable,
            Authentication authentication) {
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.getUserFromAuthentication(authentication);
            userId = user.getId();
        }

        Page<Review> reviews = reviewRepository.findByDestinationOrderByCommentCountDesc(destination, pageable);
        Long finalUserId = userId;
        return reviews.map(review -> convertToDTO(review, finalUserId));
    }

    // 리뷰 상세 조회
    @Transactional
    public ReviewDTO getReviewDetail(Long reviewId, Authentication authentication) {
        Review review = findReviewById(reviewId);

        // 조회수 증가
        review.incrementViewCount();
        reviewRepository.save(review);

        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.getUserFromAuthentication(authentication);
            userId = user.getId();
        }

        return convertToDTO(review, userId);
    }

    // 키워드로 리뷰 검색
    @Transactional(readOnly = true)
    public Page<ReviewDTO> searchReviews(String keyword, String destination, Pageable pageable,
            Authentication authentication) {
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.getUserFromAuthentication(authentication);
            userId = user.getId();
        }

        Page<Review> reviews = reviewRepository.searchByKeyword(keyword, destination, pageable);
        Long finalUserId = userId;
        return reviews.map(review -> convertToDTO(review, finalUserId));
    }

    // 태그로 리뷰 검색
    @Transactional(readOnly = true)
    public Page<ReviewDTO> searchReviewsByTags(String tagString, String destination, Pageable pageable,
            Authentication authentication) {
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.getUserFromAuthentication(authentication);
            userId = user.getId();
        }

        List<String> tags = Arrays.asList(tagString.split(","));
        Page<Review> reviews = reviewRepository.findByTagsAndDestination(tags, destination, pageable);
        Long finalUserId = userId;
        return reviews.map(review -> convertToDTO(review, finalUserId));
    }

    // 사용자가 작성한 리뷰 목록 조회
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getUserReviews(Authentication authentication, Pageable pageable) {
        User user = userService.getUserFromAuthentication(authentication);
        Page<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return reviews.map(review -> convertToDTO(review, user.getId()));
    }

    /**
     * 사용자가 특정 일정에 대해 리뷰를 작성할 수 있는지 확인
     * 
     * @param scheduleId     일정 ID
     * @param authentication 인증 정보
     * @return 리뷰 작성 가능 여부
     */
    @Transactional(readOnly = true)
    public boolean canReviewSchedule(Long scheduleId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        try {
            User user = userService.getUserFromAuthentication(authentication);
            // 이미 해당 일정에 대한 리뷰를 작성했는지 확인
            return !reviewRepository.existsByScheduleIdAndUserId(scheduleId, user.getId());
        } catch (Exception e) {
            log.error("리뷰 작성 가능 여부 확인 중 오류 발생", e);
            return false;
        }
    }

    // 리뷰 엔티티를 DTO로 변환
    private ReviewDTO convertToDTO(Review review, Long currentUserId) {
        // 댓글 목록 조회 및 변환
        List<CommentDTO> commentDTOs = new ArrayList<>();
        if (review.getComments() != null) {
            commentDTOs = review.getComments().stream()
                    .map(comment -> {
                        boolean isCommentAuthor = currentUserId != null
                                && comment.getUser().getId().equals(currentUserId);
                        String userInitial = comment.getUser().getUsername().substring(0, 1).toUpperCase();

                        return CommentDTO.builder()
                                .id(comment.getId())
                                .content(comment.getContent())
                                .createdAt(comment.getCreatedAt())
                                .updatedAt(comment.getUpdatedAt())
                                .userId(comment.getUser().getId())
                                .userName(comment.getUser().getUsername())
                                .userInitial(userInitial)
                                .reviewId(review.getId())
                                .isAuthor(isCommentAuthor)
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        // 작성자 여부 확인
        boolean isAuthor = currentUserId != null && review.getUser().getId().equals(currentUserId);

        // 작성자 이니셜 생성
        String userInitial = review.getUser().getUsername().substring(0, 1).toUpperCase();

        // viewCount가 null일 경우 0으로 처리
        Integer viewCount = review.getViewCount() != null ? review.getViewCount() : 0;

        return ReviewDTO.builder()
                .id(review.getId())
                .title(review.getTitle())
                .content(review.getContent())
                .rating(review.getRating().intValue())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .destination(review.getDestination())
                .scheduleId(review.getSchedule() != null ? review.getSchedule().getId() : null)
                .userId(review.getUser().getId())
                .userName(review.getUser().getUsername())
                .userInitial(userInitial)
                .viewCount(viewCount)
                .images(review.getImages())
                .tags(review.getTags())
                .comments(commentDTOs)
                .isAuthor(isAuthor)
                .commentCount(commentDTOs.size())
                .build();
    }

    /**
     * 모든 리뷰 목록을 조회합니다.
     * 
     * @return 모든 리뷰 목록
     */
    @Transactional(readOnly = true)
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    /**
     * 특정 ID의 리뷰를 조회합니다.
     * 
     * @param id 리뷰 ID
     * @return 리뷰 Optional 객체
     */
    @Transactional(readOnly = true)
    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    /**
     * 리뷰 ID로 리뷰 조회 (리소스가 없을 경우 예외 발생)
     * 
     * @param reviewId 리뷰 ID
     * @return Review 엔티티
     * @throws ResourceNotFoundException 리뷰가 없는 경우
     */
    private Review findReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("리뷰를 찾을 수 없습니다. ID: " + reviewId));
    }

    /**
     * 리뷰의 신고 횟수를 증가시킵니다.
     * 
     * @param id 리뷰 ID
     */
    @Transactional
    public void incrementReportCount(Long id) {
        Review review = findReviewById(id);

        review.incrementReportCount();
        reviewRepository.save(review);
        log.info("리뷰 ID {}의 신고 횟수가 증가되었습니다. 현재 신고 횟수: {}", id, review.getReportCount());
    }

    /**
     * 리뷰의 신고 횟수를 초기화합니다.
     * 
     * @param id 리뷰 ID
     */
    @Transactional
    public void resetReportCount(Long id) {
        Review review = findReviewById(id);

        review.resetReportCount();
        reviewRepository.save(review);
        log.info("리뷰 ID {}의 신고 횟수가 초기화되었습니다.", id);
    }

    /**
     * 요청자가 관리자이거나 리뷰 작성자인지 확인합니다.
     * 
     * @param authentication 인증 정보
     * @param review         리뷰 객체
     * @return 관리자이거나 작성자인 경우 true
     */
    private boolean isAdminOrAuthor(Authentication authentication, Review review) {
        if (authentication == null) {
            return false;
        }

        // 관리자 권한 확인
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return true;
        }

        // 리뷰 작성자 확인 (리뷰에 작성자 ID가 있을 경우)
        if (review.getUser() != null && review.getUser().getUsername() != null) {
            return review.getUser().getUsername().equals(authentication.getName());
        }

        return false;
    }

    /**
     * 인기 리뷰 상위 N개 조회 (메인 페이지용)
     *
     * @param limit 가져올 리뷰 개수
     * @return 인기 리뷰 목록
     */
    @Transactional(readOnly = true)
    public List<ReviewDTO> getTopReviews(int limit) {
        // 별점과 조회수를 기준으로 상위 리뷰 조회
        Pageable pageable = PageRequest.of(0, limit);
        List<Review> reviews = reviewRepository.findTopReviews(pageable);

        // DTO로 변환
        return reviews.stream()
                .map(review -> convertToDTO(review, null))
                .collect(Collectors.toList());
    }

    /**
     * 모든 리뷰 목록 조회 (필터링 가능)
     */
    @Transactional(readOnly = true)
    public Page<Review> getAllReviews(
            String category, Double minRating, Double maxRating,
            String searchTerm, Pageable pageable) {

        if (category != null || minRating != null || maxRating != null || searchTerm != null) {
            // 고급 필터링을 위한 커스텀 레포지토리 메소드 호출
            return reviewRepository.findWithFilters(category, minRating, maxRating, searchTerm, pageable);
        }

        return reviewRepository.findAll(pageable);
    }

    /**
     * 모든 리뷰 목록 조회 (기본)
     */
    @Transactional(readOnly = true)
    public Page<Review> getAllReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable);
    }

    /**
     * 신고된 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<Review> getReportedReviews(Pageable pageable) {
        return reviewRepository.findByReportCountGreaterThan(0, pageable);
    }

    /**
     * 상태별 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<Review> getReviewsByStatus(ReviewStatus status, Pageable pageable) {
        return reviewRepository.findByStatus(status, pageable);
    }

    /**
     * 리뷰 상태 변경
     */
    @Transactional
    public Review updateReviewStatus(Long id, ReviewStatus status) {
        Review review = findReviewById(id);
        review.setStatus(status);
        return reviewRepository.save(review);
    }

    /**
     * 리뷰 신고 횟수 초기화
     */
    @Transactional
    public Review clearReportCount(Long id) {
        Review review = findReviewById(id);
        review.clearReports();
        return reviewRepository.save(review);
    }

    /**
     * 관리자용 리뷰 소프트 삭제 (상태 변경)
     */
    @Transactional
    public void deleteReview(Long id) {
        Review review = findReviewById(id);
        review.setStatus(ReviewStatus.DELETED);
        reviewRepository.save(review);
    }

    /**
     * 전체 리뷰 수를 조회합니다.
     * @return 전체 리뷰 수
     */
    public long getTotalReviewsCount() {
        return reviewRepository.count();
    }

    /**
     * 최근에 작성된 리뷰 목록을 반환합니다.
     * 
     * @param limit 조회할 리뷰 수
     * @return 최근 작성된 리뷰 목록
     */
    public List<ReviewSummaryDTO> getRecentReviews(int limit) {
        List<Review> reviews = reviewRepository.findTop5ByOrderByCreatedAtDesc();
        return reviews.stream()
                .map(review -> {
                    String userName = review.getUser() != null ? review.getUser().getUsername() : "알 수 없음";
                    String title = review.getSchedule() != null ? review.getSchedule().getPlanName() : "알 수 없음";

                    return ReviewSummaryDTO.builder()
                            .id(review.getId())
                            .title(title)
                            .userName(userName)
                            .rating(review.getRating())
                            .createdAt(review.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 전체 리뷰의 평균 평점을 반환합니다.
     * 
     * @return 평균 평점
     */
    public double getAverageRating() {
        return reviewRepository.findAll().stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);
    }
}