package com.example.travel.calendar.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse {
    private String date; // 예보 날짜 (yyyyMMdd)
    private String time; // 예보 시간 (HHmm)
    private String condition; // 날씨 상태
    private String temperature; // 기온 (℃)
    private String humidity; // 습도 (%)
    private String windSpeed; // 풍속 (m/s)
    private String windDirection; // 풍향 (16방위)
    private String precipitation; // 강수량 (mm)

    // 기존 생성자 호환성 유지
    public WeatherResponse(String date, String time, String condition, String temperature, String humidity) {
        this.date = date;
        this.time = time;
        this.condition = condition;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = "N/A";
        this.windDirection = "N/A";
        this.precipitation = "0.0";
    }
}
