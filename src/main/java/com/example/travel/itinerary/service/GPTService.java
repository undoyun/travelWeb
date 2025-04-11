package com.example.travel.itinerary.service;

import com.example.travel.exception.ApiException;
import com.example.travel.itinerary.model.TravelPlan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GPTService {

  @Value("${openai.api.key}")
  private String apiKey;

  private final String GPT_API_URL = "https://api.openai.com/v1/chat/completions";
  private final int MAX_RETRIES = 3;
  private final long RETRY_DELAY_MS = 1000;

  /**
   * GPT API를 호출하여 여행 계획 내용을 생성합니다.
   *
   * @param plan 여행 계획 정보
   * @return GPT가 생성한 여행 계획 JSON 문자열
   * @throws ApiException API 호출 중 오류 발생 시
   */
  public String generatePlanContent(TravelPlan plan) {
    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper();

    int retries = 0;
    while (retries < MAX_RETRIES) {
      try {
        String prompt = generatePrompt(plan);
        log.debug("GPT 프롬프트: {}", prompt);

        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.put("model", "gpt-4");
        requestJson.put("temperature", 0.7);
        requestJson.put("max_tokens", 4000);

        ObjectNode messageNode = objectMapper.createObjectNode();
        messageNode.put("role", "system");
        messageNode.put("content", prompt);
        requestJson.putArray("messages").add(messageNode);

        String requestBody = objectMapper.writeValueAsString(requestJson);
        log.info("GPT API 요청 JSON: {}", requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            GPT_API_URL,
            HttpMethod.POST,
            entity,
            String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
          log.error("GPT API 응답 오류: {}", response.getStatusCode());
          throw new ApiException(HttpStatus.valueOf(response.getStatusCode().value()),
              "GPT API 응답 오류: " + response.getStatusCode());
        }

        if (response.getBody() == null) {
          throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "GPT 생성 실패: 응답이 없습니다.");
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode choices = root.get("choices");

        if (choices == null || !choices.isArray() || choices.size() == 0) {
          throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "GPT 생성 실패: choices 없음.");
        }

        JsonNode message = choices.get(0).get("message");
        if (message == null) {
          throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "GPT 생성 실패: message 없음.");
        }

        JsonNode contentNode = message.get("content");
        if (contentNode == null) {
          throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "GPT 생성 실패: content 없음.");
        }

        String content = contentNode.asText();
        validateJsonResponse(content);

        return content;

      } catch (RestClientException e) {
        log.error("GPT API 호출 오류 (시도 {}/{}): {}", retries + 1, MAX_RETRIES, e.getMessage());
        retries++;

        if (retries >= MAX_RETRIES) {
          throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
              "GPT API 호출 실패 (최대 재시도 횟수 초과): " + e.getMessage());
        }

        try {
          TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS * retries);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      } catch (Exception e) {
        log.error("GPT 생성 중 오류 발생: {}", e.getMessage());
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "GPT 생성 실패: " + e.getMessage());
      }
    }

    // 이 코드는 실행되지 않아야 함 (위에서 예외 발생)
    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "GPT 생성 실패: 알 수 없는 오류");
  }

  /**
   * GPT 응답이 유효한 JSON 형식인지 검증합니다.
   */
  private void validateJsonResponse(String content) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      // JSON 파싱 시도
      JsonNode jsonNode = objectMapper.readTree(content);

      // 필수 필드 확인
      if (!jsonNode.has("travelPlan")) {
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
            "GPT 응답이 올바른 형식이 아닙니다: 'travelPlan' 필드가 없습니다.");
      }

      JsonNode travelPlan = jsonNode.get("travelPlan");
      if (!travelPlan.has("itinerary")) {
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
            "GPT 응답이 올바른 형식이 아닙니다: 'itinerary' 필드가 없습니다.");
      }

    } catch (Exception e) {
      log.error("GPT 응답 JSON 검증 실패: {}", e.getMessage());
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
          "GPT 응답이 올바른 JSON 형식이 아닙니다: " + e.getMessage());
    }
  }

  /**
   * 여행 계획 정보를 바탕으로 GPT 프롬프트를 생성합니다.
   */
  private String generatePrompt(TravelPlan plan) {
    String promptTemplate = """
        당신의 역할은 사용자 정보를 바탕으로 실행 가능하고 현실적인 부산 여행 일정을 생성하는 전문가입니다. 여행자의 출발지, 목적지, 여행 기간, 테마, 예산, 이동 수단 등을 고려하여 최적의 여행 일정을 구성하세요.
        * 모든 장소는 카카오맵에서 검색 가능한 실제 부산 내 장소만 사용하세요.
        * 주소 형식은 간단하게 '부산 + 지역명(구/동)' 또는 '정확한 장소명'만 사용하세요.
        * 각 일정은 시간대별로 잘 구성하고, 이동 시간을 고려하세요.
        * 아침/점심/저녁 식사를 모두 포함하세요.

        여행 계획은 반드시 다음 기준을 준수하여 구성하세요:

        1. 장소 선정 규칙
        - 부산 내 실제 장소만 사용하며, 모든 장소는 카카오맵에서 검색 가능해야 함
        - 장소명은 간결하게 '해운대해수욕장', '부산 국제시장' 형태로 사용
        - 프랜차이즈 점포는 지점명 포함 (예: '스타벅스 해운대점')

        2. 일정 구성 규칙
        - 첫날은 출발지에서 부산 도착으로 시작 (기차는 부산역, 비행기는 김해공항, 버스는 부산종합버스터미널)
        - 둘째 날부터 마지막 날 전날까지는 일정 시작과 종료에 숙소가 포함되어야 함
        - 마지막 날은 출발 교통수단에 맞게 부산역/김해공항/부산종합버스터미널에서 끝나도록 구성
        - 마지막 날은 절대 일정에 숙소가 포함되지 않도록 구성
        - 숙소 체크인은 14-16시 사이에 포함
        - 여행 시작일부터 종료일까지 모든 날의 일정을 빠짐없이 생성해야 함

        3. 식사 구성
        - 일자별로 아침, 점심, 저녁 식사를 모두 포함하고 실제 식당명 사용
        - 주변 지역 유명 식당을 추천하되, 이동 시간을 고려하여 구성

        4. JSON 형식 요구사항
        - 간결하고 명확한 JSON 형식 유지
        - 주소(address) 필드는 생략하고 location 필드만 포함
        - category 필드는 '출발', '숙소', '관광', '식사', '귀가' 중 하나만 사용
        - 반드시 아래 제공된 예시와 정확히 동일한 JSON 구조로 생성할 것
        - 절대로 구조를 변경하거나 필드를 추가/삭제하지 말 것
        - 반드시 전체 여행 기간에 맞는 일수만큼의 일정을 생성해야 함 (시작일부터 종료일까지)

        [입력 정보]
        - 출발 장소: %s
        - 출발 날짜: %s
        - 출발 시간: %s
        - 종료 날짜: %s
        - 도착 시간: %s
        - 교통 수단: %s
        - 도착 장소: %s
        - 여행 목적: %s
        - 여행 예산: %d
        - 여행 밀도: %s
        - 숙소 위치: %s
        - 이동 방식: %s

        **출력 JSON 예시**
        {
          "travelPlan": {
            "plan_Id": "e2f82b7c-621c-4a31-ae39-fa5d6f4d3c6a",
            "departLocation": "서울",
            "startDate": "2025-04-16",
            "departureTime": "09:00",
            "endDate": "2025-04-19",
            "returnTime": "18:00",
            "transportation": "기차",
            "destination": "부산",
            "purpose": "관광",
            "budget": 700000,
            "density": "적당히",
            "hotelLocation": "부산 신라스테이 해운대점",
            "travelMode": "대중교통",
            "itinerary": [
              {
                "date": "2025-04-16",
                "weather": "맑음",
                "totalCost": "50,000원",
                "schedule": [
                  {
                    "departureTime": "11:00",
                    "category": "출발",
                    "location": "부산역",
                    "duration": "30분",
                    "cost": "지하철"
                  },
                  {
                    "departureTime": "14:00",
                    "category": "관광",
                    "location": "부산 국제시장",
                    "duration": "2시간",
                    "cost": "도보"
                  },
                  {
                    "departureTime": "17:30",
                    "category": "식사",
                    "location": "부산 흑돈가 서면본점",
                    "duration": "1시간",
                    "cost": "식사"
                  },
                  {
                    "departureTime": "19:00",
                    "category": "숙소",
                    "location": "부산 신라스테이 해운대점",
                    "duration": "도보",
                    "cost": "무료"
                  }
                ]
              },
              {
                "date": "2025-04-17",
                "weather": "맑음",
                "totalCost": "70,000원",
                "schedule": [
                  {
                    "departureTime": "09:00",
                    "category": "숙소",
                    "location": "부산 신라스테이 해운대점",
                    "duration": "도보",
                    "cost": "무료"
                  },
                  {
                    "departureTime": "10:00",
                    "category": "관광",
                    "location": "해운대해수욕장",
                    "duration": "3시간",
                    "cost": "해변 휴식"
                  },
                  {
                    "departureTime": "13:00",
                    "category": "식사",
                    "location": "해운대 밀면",
                    "duration": "1시간",
                    "cost": "식사 15,000원"
                  },
                  {
                    "departureTime": "15:00",
                    "category": "관광",
                    "location": "부산 영화의전당",
                    "duration": "2시간",
                    "cost": "입장료 5,000원"
                  },
                  {
                    "departureTime": "18:00",
                    "category": "식사",
                    "location": "부산 서면 소문난 닭한마리",
                    "duration": "1시간 30분",
                    "cost": "식사 30,000원"
                  },
                  {
                    "departureTime": "19:00",
                    "category": "숙소",
                    "location": "부산 신라스테이 해운대점",
                    "duration": "도보",
                    "cost": "무료"
                  }
                ]
              },
              {
                "date": "2025-04-18",
                "weather": "흐림",
                "totalCost": "65,000원",
                "schedule": [
                  {
                    "departureTime": "09:00",
                    "category": "숙소",
                    "location": "부산 신라스테이 해운대점",
                    "duration": "도보",
                    "cost": "무료"
                  },
                  {
                    "departureTime": "10:00",
                    "category": "관광",
                    "location": "광안리해수욕장",
                    "duration": "2시간",
                    "cost": "무료"
                  },
                  {
                    "departureTime": "12:30",
                    "category": "식사",
                    "location": "부산 광안리 소금구이",
                    "duration": "1시간",
                    "cost": "식사 25,000원"
                  },
                  {
                    "departureTime": "14:00",
                    "category": "관광",
                    "location": "부산 영도대교",
                    "duration": "1시간",
                    "cost": "무료"
                  },
                  {
                    "departureTime": "19:00",
                    "category": "숙소",
                    "location": "부산 신라스테이 해운대점",
                    "duration": "도보",
                    "cost": "무료"
                  }
                ]
              },
              {
                "date": "2025-04-19",
                "weather": "맑음",
                "totalCost": "40,000원",
                "schedule": [
                  {
                    "departureTime": "09:00",
                    "category": "숙소",
                    "location": "부산 신라스테이 해운대점",
                    "duration": "도보",
                    "cost": "무료"
                  },
                  {
                    "departureTime": "10:00",
                    "category": "관광",
                    "location": "부산 감천문화마을",
                    "duration": "2시간",
                    "cost": "입장료 10,000원"
                  },
                  {
                    "departureTime": "12:30",
                    "category": "식사",
                    "location": "부산 기장 해물탕",
                    "duration": "1시간",
                    "cost": "식사 30,000원"
                  },
                  {
                    "departureTime": "16:00",
                    "category": "귀가",
                    "location": "부산역",
                    "duration": "2시간",
                    "cost": "기차"
                  }
                ]
              }
            ]
          }
        }

        위 조건과 지침을 철저히 따라 여행 계획을 생성해 주세요.
        다음 사항을 반드시 지켜주세요:
        1. 정확히 위 예시와 같은 JSON 구조로 출력할 것
        2. 절대 address 필드는 포함하지 말고 location 필드만 사용할 것
        3. 카테고리는 '출발', '숙소', '관광', '식사', '귀가'만 사용할 것
        4. 여행 첫날과 마지막 날 규칙을 엄격히 준수할 것
        5. 반드시 출발 날짜부터 종료 날짜까지 모든 날짜의 일정을 빠짐없이 생성할 것
        """;
    return String.format(promptTemplate,
        plan.getDepartLocation(),
        plan.getStartDate(),
        plan.getDepartureTime(),
        plan.getEndDate(),
        plan.getReturnTime(),
        plan.getTransportation(),
        (plan.getDestination() != null ? plan.getDestination() : "N/A"),
        plan.getPurpose(),
        plan.getBudget(),
        plan.getDensity(),
        plan.getHotelLocation(),
        plan.getTravelMode());
  }

}