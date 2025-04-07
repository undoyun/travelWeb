package com.example.travel.calendar.model;

import com.example.travel.schedule.model.Schedule;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDTO {
    private Long id;

    @JsonProperty("title") // ✅ JSON에서 "title" 키로 변환
    private String title;

    private String start;
    private String end;
    private String description;

    // ✅ Schedule 데이터를 EventDTO로 변환
    public CalendarDTO(Schedule schedule) {
        this.id = schedule.getId();
        this.title = schedule.getPlanName(); // ✅ 기존 destination → planName 변경
        this.start = schedule.getStartDate();
        this.end = schedule.getEndDate();
        this.description = schedule.getPlanDescription(); // ✅ planDescription을 불러오기!
    }
}
