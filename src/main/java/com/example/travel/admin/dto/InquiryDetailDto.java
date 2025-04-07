package com.example.travel.admin.dto;

import com.example.travel.counseling.model.Counseling;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InquiryDetailDto {
    private Long id;
    private String title;
    private String content;
    private String writer;
    private String email;
    private String contactNumber;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String answer;
    private String answeredBy;
    private LocalDateTime answeredAt;
    private boolean isAnswered;
    private boolean isPrivate;
    
    /**
     * 문의(Counseling) 엔티티를 InquiryDetailDto로 변환합니다.
     */
    public static InquiryDetailDto fromEntity(Counseling counseling) {
        return InquiryDetailDto.builder()
                .id(counseling.getId())
                .title(counseling.getTitle())
                .content(counseling.getContent())
                .writer(counseling.getUser() != null ? counseling.getUser().getUsername() : "알 수 없음")
                .email(counseling.getUser() != null ? counseling.getUser().getEmail() : null)
                .contactNumber(null) // 임시로 null 처리, 실제 값은 Counseling 엔터티에서 가져와야 함
                .category(counseling.getCategory().getDisplayName())
                .createdAt(counseling.getCreatedAt())
                .updatedAt(counseling.getUpdatedAt())
                .answer(counseling.getAnswer())
                .answeredBy(counseling.getAnsweredBy())
                .answeredAt(counseling.getAnsweredAt())
                .isAnswered(counseling.isAnswered())
                .isPrivate(counseling.isPrivate())
                .build();
    }
} 