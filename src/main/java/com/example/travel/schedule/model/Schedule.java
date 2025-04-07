package com.example.travel.schedule.model;

import com.example.travel.review.model.Review;
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
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "schedule")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

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

    @Column(nullable = false)
    private String departureTime;

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

    // 최종 편집된 일정(JSON)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String scheduleJson;

    // 모달에서 입력받은 정보들
    @NotBlank(message = "여행 이름은 필수 입력 항목입니다")
    @Column(nullable = false)
    private String planName; // 최종 여행 이름

    private String planPhoto; // 사진 URL
    private String planDescription; // 여행 설명

    // 생성 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 리뷰와의 관계 설정
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "schedule_id")
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @PrePersist
    public void onPrePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 일정 ID를 반환합니다.
     * 
     * @return 일정 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 일정 ID를 설정합니다.
     * 
     * @param id 설정할 일정 ID
     */
    public void setId(Long id) {
        this.id = id;
    }
}
