package com.example.travel.schedule.dto;

import com.example.travel.schedule.model.Schedule;
import com.example.travel.user.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDTO {

    private Long id;
    private String userId;

    @NotBlank(message = "출발지를 입력해주세요")
    private String departLocation;

    @NotBlank(message = "출발 날짜를 입력해주세요")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "날짜 형식은 YYYY-MM-DD여야 합니다")
    private String startDate;

    @NotBlank(message = "종료 날짜를 입력해주세요")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "날짜 형식은 YYYY-MM-DD여야 합니다")
    private String endDate;

    @NotBlank(message = "출발 시간을 입력해주세요")
    @Pattern(regexp = "([01]?[0-9]|2[0-3]):[0-5][0-9]", message = "시간 형식은 HH:MM이어야 합니다")
    private String departureTime;

    @NotBlank(message = "도착 시간을 입력해주세요")
    @Pattern(regexp = "([01]?[0-9]|2[0-3]):[0-5][0-9]", message = "시간 형식은 HH:MM이어야 합니다")
    private String returnTime;

    @NotBlank(message = "교통 수단을 선택해주세요")
    private String transportation;

    @NotBlank(message = "목적지를 입력해주세요")
    private String destination;

    @NotBlank(message = "여행 목적을 선택해주세요")
    private String purpose;

    @Min(value = 0, message = "예산은 0 이상이어야 합니다")
    private int budget;

    private String density;
    private String hotelLocation;
    private String travelMode;

    @NotBlank(message = "일정 정보가 필요합니다")
    private String scheduleJson;

    @NotBlank(message = "여행 이름을 입력해주세요")
    private String planName;

    private String planPhoto;
    private String planDescription;

    private LocalDateTime createdAt;

    // 리뷰 작성 여부를 추적하는 필드 - 메인 페이지 표시용
    private boolean hasReview;

    // 여행 제목 - 메인 페이지에서 표시할 제목
    @JsonIgnore
    public String getTitle() {
        return this.planName;
    }

    // 여행까지 남은 일수
    @JsonIgnore
    public long getDaysUntil() {
        LocalDate today = LocalDate.now();
        LocalDate start = LocalDate.parse(this.startDate);
        return today.until(start, ChronoUnit.DAYS);
    }

    // 여행 기간 (일수)
    @JsonIgnore
    public int getDuration() {
        LocalDate start = LocalDate.parse(this.startDate);
        LocalDate end = LocalDate.parse(this.endDate);
        return Period.between(start, end).getDays() + 1; // 시작일과 종료일 포함
    }

    // ScheduleDTO -> Schedule 변환 메서드
    public Schedule toEntity() {
        Schedule schedule = new Schedule();
        schedule.setId(this.id);
        
        // User 객체 설정
        if (this.userId != null) {
            User user = new User();
            user.setId(Long.parseLong(this.userId));
            schedule.setUser(user);
        }
        
        schedule.setDepartLocation(this.departLocation);
        schedule.setStartDate(this.startDate);
        schedule.setEndDate(this.endDate);
        schedule.setDepartureTime(this.departureTime);
        schedule.setReturnTime(this.returnTime);
        schedule.setTransportation(this.transportation);
        schedule.setDestination(this.destination);
        schedule.setPurpose(this.purpose);
        schedule.setBudget(this.budget);
        schedule.setDensity(this.density);
        schedule.setHotelLocation(this.hotelLocation);
        schedule.setTravelMode(this.travelMode);
        schedule.setScheduleJson(this.scheduleJson);
        schedule.setPlanName(this.planName);
        schedule.setPlanPhoto(this.planPhoto);
        schedule.setPlanDescription(this.planDescription);
        schedule.setCreatedAt(this.createdAt);
        return schedule;
    }

    // Schedule -> ScheduleDTO 변환 메서드
    public static ScheduleDTO fromEntity(Schedule schedule) {
        return ScheduleDTO.builder()
                .id(schedule.getId())
                .userId(schedule.getUser() != null ? schedule.getUser().getId().toString() : null)
                .departLocation(schedule.getDepartLocation())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .departureTime(schedule.getDepartureTime())
                .returnTime(schedule.getReturnTime())
                .transportation(schedule.getTransportation())
                .destination(schedule.getDestination())
                .purpose(schedule.getPurpose())
                .budget(schedule.getBudget())
                .density(schedule.getDensity())
                .hotelLocation(schedule.getHotelLocation())
                .travelMode(schedule.getTravelMode())
                .scheduleJson(schedule.getScheduleJson())
                .planName(schedule.getPlanName())
                .planPhoto(schedule.getPlanPhoto())
                .planDescription(schedule.getPlanDescription())
                .createdAt(schedule.getCreatedAt())
                .build();
    }
}