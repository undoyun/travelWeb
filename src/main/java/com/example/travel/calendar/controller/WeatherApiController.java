package com.example.travel.calendar.controller;

import com.example.travel.calendar.model.WeatherResponse;
import com.example.travel.calendar.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherApiController {
    private static final Logger logger = LoggerFactory.getLogger(WeatherApiController.class);
    private final WeatherService weatherService;

    public WeatherApiController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/api/weather")
    public ResponseEntity<WeatherResponse> getWeatherApi(
            @RequestParam(name = "lat") double lat,
            @RequestParam(name = "lon") double lon) {

        logger.info("날씨 API 요청 - 위도: {}, 경도: {}", lat, lon);

        // 위도, 경도를 기상청 격자 좌표(nx, ny)로 변환
        int[] grid = convertToGridCoord(lat, lon);
        int nx = grid[0];
        int ny = grid[1];

        logger.info("격자 좌표 변환 결과 - nx: {}, ny: {}", nx, ny);

        WeatherResponse weather = weatherService.getWeather(nx, ny);
        logger.info("날씨 데이터 응답: {}", weather);

        return ResponseEntity.ok(weather);
    }

    // 위도/경도를 기상청 좌표로 변환 (LCC DFS 좌표변환)
    private int[] convertToGridCoord(double lat, double lon) {
        double RE = 6371.00877; // 지구 반경(km)
        double GRID = 5.0; // 격자 간격(km)
        double SLAT1 = 30.0; // 투영 위도1(degree)
        double SLAT2 = 60.0; // 투영 위도2(degree)
        double OLON = 126.0; // 기준점 경도(degree)
        double OLAT = 38.0; // 기준점 위도(degree)
        double XO = 43; // 기준점 X좌표(GRID)
        double YO = 136; // 기준점 Y좌표(GRID)

        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = (Math.pow(sf, sn) * Math.cos(slat1)) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = (re * sf) / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
        ra = (re * sf) / Math.pow(ra, sn);
        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI)
            theta -= 2.0 * Math.PI;
        if (theta < -Math.PI)
            theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new int[] { nx, ny };
    }
}