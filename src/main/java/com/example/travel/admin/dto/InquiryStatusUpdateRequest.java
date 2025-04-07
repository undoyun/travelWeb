package com.example.travel.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryStatusUpdateRequest {
    private String adminId;
    private boolean currentStatus;
} 