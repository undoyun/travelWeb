package com.example.travel.review.service;

import com.example.travel.exception.ResourceNotFoundException;
import com.example.travel.exception.UnauthorizedAccessException;
import com.example.travel.review.dto.CommentDTO;
import com.example.travel.review.model.Comment;
import com.example.travel.review.model.Review;
import com.example.travel.review.repository.CommentRepository;
import com.example.travel.review.repository.ReviewRepository;
import com.example.travel.user.model.User;
import com.example.travel.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final UserService userService;

    /**
     * 댓글 저장
     */
    @Transactional
    public CommentDTO saveComment(CommentDTO commentDTO, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Review review = reviewRepository.findById(commentDTO.getReviewId())
                .orElseThrow(() -> new ResourceNotFoundException("리뷰를 찾을 수 없습니다. ID: " + commentDTO.getReviewId()));

        Comment comment = Comment.builder()
                .content(commentDTO.getContent())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .user(user)
                .review(review)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return convertToDTO(savedComment, user.getId());
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentDTO updateComment(Long commentId, CommentDTO commentDTO, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Comment comment = getCommentById(commentId);

        // 작성자 확인
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        comment.setContent(commentDTO.getContent());
        comment.setUpdatedAt(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);
        return convertToDTO(updatedComment, user.getId());
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long commentId, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Comment comment = getCommentById(commentId);

        // 작성자 확인
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }

    /**
     * 리뷰별 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByReviewId(Long reviewId, Authentication authentication) {
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.findByUsername(authentication.getName());
            userId = user.getId();
        }

        List<Comment> comments = commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId);
        Long finalUserId = userId;
        return comments.stream()
                .map(comment -> convertToDTO(comment, finalUserId))
                .collect(Collectors.toList());
    }

    /**
     * 리뷰별 댓글 목록 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<CommentDTO> getCommentsByReviewIdPaged(Long reviewId, Pageable pageable,
            Authentication authentication) {
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.findByUsername(authentication.getName());
            userId = user.getId();
        }

        Page<Comment> comments = commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId, pageable);
        Long finalUserId = userId;
        return comments.map(comment -> convertToDTO(comment, finalUserId));
    }

    /**
     * 사용자별 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommentDTO> getUserComments(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        List<Comment> comments = commentRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return comments.stream()
                .map(comment -> convertToDTO(comment, user.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 댓글 ID로 댓글 조회
     */
    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다. ID: " + commentId));
    }

    /**
     * 댓글 엔티티를 DTO로 변환
     */
    private CommentDTO convertToDTO(Comment comment, Long currentUserId) {
        boolean isAuthor = currentUserId != null && comment.getUser().getId().equals(currentUserId);
        String userInitial = comment.getUser().getUsername().substring(0, 1).toUpperCase();

        return CommentDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getUsername())
                .userInitial(userInitial)
                .reviewId(comment.getReview().getId())
                .isAuthor(isAuthor)
                .build();
    }
}