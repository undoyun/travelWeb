package com.example.travel.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InquiryDto {
    private Long id;               // 문의 고유번호
    private String title;          // 문의 제목
    private String category;       // 문의 카테고리
    private String writer;         // 작성자 (또는 사용자 아이디)
    private LocalDateTime createdAt; // 작성일시
    private boolean isAnswered;    // 답변 여부
}
