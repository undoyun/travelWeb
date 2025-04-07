package com.example.travel.review.controller;

import com.example.travel.review.dto.CommentDTO;
import com.example.travel.review.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 리뷰에 댓글 작성 (MVC 방식)
     */
    @PostMapping("/reviews/{reviewId}")
    public String saveCommentMvc(
            @PathVariable Long reviewId,
            @RequestParam String content,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            CommentDTO commentDTO = new CommentDTO();
            commentDTO.setReviewId(reviewId);
            commentDTO.setContent(content);

            commentService.saveComment(commentDTO, authentication);
        } catch (Exception e) {
            log.error("댓글 저장 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 저장 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/reviews/" + reviewId;
    }

    /**
     * 댓글 수정 (MVC 방식)
     */
    @PostMapping("/{commentId}/update")
    public String updateCommentMvc(
            @PathVariable Long commentId,
            @RequestParam String content,
            @RequestParam Long reviewId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            CommentDTO commentDTO = new CommentDTO();
            commentDTO.setContent(content);

            commentService.updateComment(commentId, commentDTO, authentication);
        } catch (Exception e) {
            log.error("댓글 수정 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 수정 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/reviews/" + reviewId;
    }

    /**
     * 댓글 삭제 (MVC 방식)
     */
    @PostMapping("/{commentId}/delete")
    public String deleteCommentMvc(
            @PathVariable Long commentId,
            @RequestParam Long reviewId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            commentService.deleteComment(commentId, authentication);
        } catch (Exception e) {
            log.error("댓글 삭제 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/reviews/" + reviewId;
    }
}