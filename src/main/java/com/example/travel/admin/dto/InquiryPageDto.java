package com.example.travel.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InquiryPageDto {
    private List<InquiryDto> content; // 현재 페이지 문의 목록
    private int totalPages; // 총 페이지 수
    private long totalElements; // 총 문의 건수
    private int currentPage; // 현재 페이지 번호 (0-based)

    // 이전 페이지가 있는지 확인
    public boolean hasPrevious() {
        return currentPage > 0;
    }

    // 다음 페이지가 있는지 확인
    public boolean hasNext() {
        return currentPage < totalPages - 1;
    }

    // 현재 페이지 번호 (PageImpl 호환성을 위해 getNumber() 메소드 추가)
    public int getNumber() {
        return currentPage;
    }
}
