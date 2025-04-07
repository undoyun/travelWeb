package com.example.travel.itinerary.model;

import com.example.travel.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "travel_plan")
public class TravelPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id", unique = true, nullable = false)
    private Long planId; // 타입이 Long인지 확인

    // 출발/도착/기간/예산 등등 (GPT 생성 시 필요한 정보)
    @NotBlank(message = "출발지는 필수 입력 항목입니다")
    @Column(nullable = false)
    private String departLocation;

    @NotBlank(message = "출발 날짜는 필수 입력 항목입니다")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "날짜 형식은 YYYY-MM-DD여야 합니다")
    @Column(nullable = false)
    private String startDate;

    @NotBlank(message = "종료 날짜는 필수 입력 항목입니다")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "날짜 형식은 YYYY-MM-DD여야 합니다")
    @Column(nullable = false)
    private String endDate;

    @NotBlank(message = "출발 시간은 필수 입력 항목입니다")
    @Pattern(regexp = "([01]?[0-9]|2[0-3]):[0-5][0-9]", message = "시간 형식은 HH:MM이어야 합니다")
    @Column(nullable = false)
    private String departureTime;

    @NotBlank(message = "도착 시간은 필수 입력 항목입니다")
    @Pattern(regexp = "([01]?[0-9]|2[0-3]):[0-5][0-9]", message = "시간 형식은 HH:MM이어야 합니다")
    @Column(nullable = false)
    private String returnTime;

    @NotBlank(message = "교통 수단을 선택해주세요")
    @Column(nullable = false)
    private String transportation;

    @NotBlank(message = "목적지는 필수 입력 항목입니다")
    @Column(nullable = false)
    private String destination;

    @NotBlank(message = "여행 목적을 선택해주세요")
    @Column(nullable = false)
    private String purpose;

    @Min(value = 0, message = "예산은 0 이상이어야 합니다")
    @Column(nullable = false)
    private int budget;

    private String density;
    private String hotelLocation;
    private String travelMode;

    // GPT가 생성한 일정(JSON)
    @Column(columnDefinition = "TEXT")
    private String itinerary;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "FK_USER_ID"))
    private User user;

    // 생성 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 편집(확정) 완료 여부
    @Column(name = "completed", columnDefinition = "BOOLEAN DEFAULT false", nullable = false)
    @Builder.Default
    private boolean isCompleted = false;

    // 위치 정보와의 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @PrePersist
    public void onPrePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
