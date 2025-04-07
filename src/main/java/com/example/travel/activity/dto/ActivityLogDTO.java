package com.example.travel.activity.dto;

import com.example.travel.activity.model.ActivityType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ActivityLogDTO {
    private Long id;
    private Long userId;
    private String userName;
    private ActivityType activityType;
    private String description;
    private Long targetId;
    private String targetType;
    private LocalDateTime createdAt;

    @Builder
    public ActivityLogDTO(Long id, Long userId, String userName, ActivityType activityType, 
                    String description, Long targetId, String targetType, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.activityType = activityType;
        this.description = description;
        this.targetId = targetId;
        this.targetType = targetType;
        this.createdAt = createdAt;
    }
} 