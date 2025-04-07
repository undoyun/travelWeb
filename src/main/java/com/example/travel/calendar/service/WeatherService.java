package com.example.travel.calendar.service;

import com.example.travel.calendar.model.WeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private static final String API_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst";

    @Value("${weather.api.key}")
    private String apiKey;

    public WeatherResponse getWeather(int nx, int ny) {
        try {
            // 현재 시간에서 30분을 빼서 발표 시간으로 사용
            LocalDateTime t = LocalDateTime.now().minusMinutes(30);
            String baseDate = t.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String baseTime = t.format(DateTimeFormatter.ofPattern("HHmm"));

            // 기상청 API URL 구성
            String apiUrlWithParams = API_URL +
                    "?ServiceKey=" + apiKey +
                    "&numOfRows=60" +
                    "&pageNo=1" +
                    "&dataType=XML" +
                    "&base_date=" + baseDate +
                    "&base_time=" + baseTime +
                    "&nx=" + nx +
                    "&ny=" + ny;

            logger.info("기상청 API 요청: {}", apiUrlWithParams);

            // API 연결 및 응답 파싱
            URL url = new URL(apiUrlWithParams);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            // 응답 코드 확인
            int responseCode = con.getResponseCode();
            logger.info("기상청 API 응답 코드: {}", responseCode);

            if (responseCode != 200) {
                logger.error("기상청 API 응답 실패: {}", responseCode);
                return new WeatherResponse("Error", "Error", "API 응답 실패", "N/A", "N/A", "N/A", "N/A", "0.0");
            }

            // XML 응답 파싱
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(con.getInputStream());

            // 결과 코드 확인
            NodeList resultCode = doc.getElementsByTagName("resultCode");
            if (resultCode.getLength() > 0) {
                String code = resultCode.item(0).getTextContent();
                if (!"00".equals(code)) {
                    String resultMsg = doc.getElementsByTagName("resultMsg").item(0).getTextContent();
                    logger.error("기상청 API 오류: {} - {}", code, resultMsg);
                    return new WeatherResponse("Error", "Error", resultMsg, "N/A", "N/A", "N/A", "N/A", "0.0");
                }
            }

            // 날씨 정보 변수 초기화
            String date = null, time = null, condition = "알 수 없음";
            String temperature = "N/A", humidity = "N/A", windSpeed = "N/A", windDirection = "N/A",
                    precipitation = "0.0";
            String pty = null, sky = null;

            // 응답 아이템 파싱
            NodeList items = doc.getElementsByTagName("item");
            logger.info("기상청 API 응답 아이템 수: {}", items.getLength());

            for (int i = 0; i < items.getLength(); i++) {
                Element e = (Element) items.item(i);

                if (date == null) {
                    date = e.getElementsByTagName("fcstDate").item(0).getTextContent();
                    time = e.getElementsByTagName("fcstTime").item(0).getTextContent();
                }

                String category = e.getElementsByTagName("category").item(0).getTextContent();
                String value = e.getElementsByTagName("fcstValue").item(0).getTextContent();

                logger.debug("카테고리: {}, 값: {}", category, value);

                if ("PTY".equals(category))
                    pty = value;
                else if ("SKY".equals(category))
                    sky = value;
                else if ("T1H".equals(category))
                    temperature = value;
                else if ("REH".equals(category))
                    humidity = value;
                else if ("WSD".equals(category))
                    windSpeed = value;
                else if ("VEC".equals(category))
                    windDirection = convertWindDirection(value);
                else if ("RN1".equals(category))
                    precipitation = value.equals("강수없음") ? "0.0" : value;
            }

            // 날씨 상태 결정
            if (pty != null && sky != null) {
                if ("0".equals(pty)) {
                    if ("1".equals(sky))
                        condition = "맑음";
                    else if ("3".equals(sky))
                        condition = "구름많음";
                    else if ("4".equals(sky))
                        condition = "흐림";
                } else if ("1".equals(pty))
                    condition = "비";
                else if ("2".equals(pty))
                    condition = "비/눈";
                else if ("3".equals(pty))
                    condition = "눈";
                else if ("4".equals(pty))
                    condition = "소나기";
            }

            con.disconnect();

            WeatherResponse response = new WeatherResponse(date, time, condition, temperature, humidity,
                    windSpeed, windDirection, precipitation);
            logger.info("날씨 정보 응답: {}", response);
            return response;

        } catch (Exception e) {
            logger.error("날씨 정보 요청 중 오류 발생: {}", e.getMessage(), e);
            return new WeatherResponse("Error", "Error", e.getMessage(), "N/A", "N/A", "N/A", "N/A", "0.0");
        }
    }

    // 풍향 각도를 16방위로 변환하는 유틸리티 메서드
    private String convertWindDirection(String degree) {
        try {
            int deg = Integer.parseInt(degree);
            String[] directions = { "북", "북북동", "북동", "동북동", "동", "동남동", "남동", "남남동",
                    "남", "남남서", "남서", "서남서", "서", "서북서", "북서", "북북서", "북" };
            int index = (int) Math.round(deg / 22.5) % 16;
            return directions[index];
        } catch (NumberFormatException e) {
            logger.error("풍향 변환 오류: {}", e.getMessage());
            return "N/A";
        }
    }
}
