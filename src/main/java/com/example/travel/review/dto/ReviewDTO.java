package com.example.travel.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {

    private Long id;
    private String title;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String destination;
    private Long scheduleId;
    private Long userId;
    private String userName;
    private String userInitial;
    private Integer viewCount;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Builder.Default
    private List<CommentDTO> comments = new ArrayList<>();

    private boolean isAuthor;
    private int commentCount;
}