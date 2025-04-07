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
        requestJson.put("model", "gpt-3.5-turbo");
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
    return """

                당신의 역할은 사용자 정보를 바탕으로 실행 가능한 고품질의 현실적인 여행 일정을 생성하는 전문가 입니다. 여행자의 출발지와 목적지, 여행 기간, 여행테마, 예상 예산, 이동 수단 등을 고려하여, 다음 조건을 충족하는 여행 일정을 구성하세요.
                * 논리적이고 현실적인 흐름: 시간대별로 이동과 활동이 자연스럽게 연결되도록 구성
                * 효율적인 이동 동선: 교통편(도보, 대중교통, 차량 등)을 고려한 최적의 동선 설계
                * 적절한 활동 밀도: 여행자의 체력과 여행 스타일(느긋한 여행 vs. 빠르게 둘러보기)에 맞춘 일정 조정
                * 현지의 특성을 반영한 추천: 해당 지역의 유명 명소뿐만 아니라, 숨은 명소나 로컬 추천 장소 포함
                * 식사 및 휴식 시간 고려: 현지 인기 맛집과 카페 정확한 장소 추천, 무리 없는 일정 조정
                최종 일정은 여행자가 쉽게 참고할 수 있도록 교통편 정보(소요 시간, 이동 방법)도 포함하세요.

                여행 계획은 반드시 다음 기준을 엄격하게 반드시 지켜 구성하세요:

                1. 부산 내 실제 장소만 사용
                - 일정에 포함되는 모든 장소는 부산광역시 내 주소를 가진 장소여야 합니다.
                - 주소지 기준 우편번호가 46~49로 시작하는 장소만 포함할 수 있습니다.
                - 다른 지역(예: 서울)에 동일한 이름이 있는 장소는 포함할 수 없습니다.
                - 모든 장소는 반드시 카카오맵에서 실제로 검색 가능한 명칭이어야 합니다.
                - 프랜차이즈 카페나 음식점의 경우 반드시 지점명을 포함하여 표기하세요.
                  - 예: ‘스타벅스 부산 해운대점’, ‘이디야커피 광안리해수욕장점’
                - 추상적이고 일반적인 표현(예: ‘현지 맛집’, ‘분위기 좋은 카페’)은 절대 사용하지 마세요.
                - 예를 들어 ‘송도해수욕장’은 부산 송도만 포함할 수 있으며, 서울 송도는 절대 포함 불가입니다.

                2. 여행 일정 구성 기본 규칙(반드시 지키기)
                - 도착일 첫날은 교통수단이 ‘기차’, ‘비행기’, ‘버스’ 인경우 기차는 서울역, 비행기는 공항, 버스는 터미널에서 일정이 시작하며 그 외의 수단일 경우 출발지 언급없이 관광지부터 시작해주세요.
                - 숙소 체크인은 14시 이후에 절적하게 일정에 넣어 구성하세요.
                - 둘째 날부터 마지막 전날까지는 절대적으로 일정 시작이 숙소에서 시작해 숙소로 돌아오는 일정으로 구성하고, 각 일자마다 동일한 지역 중심(예: 해운대구 중심, 중구 중심 등)으로 구성하세요.
                - 마지막 날은 반드시 숙소에서 출발하되, 마무리는 교통 수단에 따라 지정된 장소로 종료하세요:
                  - 기차: 부산역 / 버스: 부산사상버스터미널 / 비행기: 김해공항
                - 각 일정은 이동 동선을 고려하여 효율적으로 구성하며, 도보·대중교통·차량 등의 이동 시간을 반영해 주세요.

                3. 식사·카페도 반드시 같은 지역 내에서 선정
                - 관광지 지역과 다른 지역의 식당/카페는 포함하지 마세요.
                - 관광명소 근처 식당/카페는 포함하되, 관광명소 근처 식당/카페가 없는 경우 관광명소 근처 식당/카페를 포함하세요.
                - 하루 식사는 3번 이상 포함하세요.

                4. 계절·밀도·예산 반영
                - 출발 날짜 기준 계절(봄/여름/가을/겨울)에 맞는 활동을 포함하세요.
                - 예산과 여행 밀도(적당히, 알차게 등)를 반영하여 하루 3~5개 일정으로 구성하세요.
                - 각 일정에는 예상 비용(입장료, 식사비, 교통비 등)을 꼭 명시하세요.

                5. 비상 연락처 및 시설 정보
                - 여행 중 응급 상황에 대비한 가까운 병원, 약국, 경찰서 등의 위치 정보를 각 일차별로 포함해주세요.
                - 한국어 사용 가능한 서비스나 통역 서비스 정보를 추가해주세요.

                6. 식사 시간 포함
                - 아침(08~10시), 점심(12~13시), 저녁(18~20시)에 식사 일정을 포함해 주세요.

                7. 비용 정보 포함
                - 각 일정 항목에는 반드시 비용 정보를 포함하세요:
                - 교통: 탑승 요금 (예: 기차 KTX 요금 56,000원 등)
                - 관광지: 입장료 또는 체험 비용
                - 식사/카페: 예상 식사 비용

                8. 숙소 정보 포함
                - 여행자가 숙박하는 지역(예: 남포동, 해운대 등)에 맞는 추천 호텔을 포함하세요.
                - 호텔 이름은 반드시 카카오맵에서 검색 가능한 호텔명을 사용하세요.
                - 호텔 좌표는 반드시 부산광역시 내 좌표를 사용하세요.

                9. JSON 형식은 정확하게 유지
                - 아래 JSON 형식을 반드시 준수해 주세요. 누락되거나 잘못된 구조는 허용되지 않습니다.

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
                            "plan_Id": "UUID",
                            "departLocation": "출발 장소",
                            "startDate": "출발 날짜",
                            "departureTime": "출발 시간",
                            "endDate": "종료 날짜",
                            "returnTime": "도착 시간",
                            "transportation": "교통 수단",
                            "destination": "도착 장소",
                            "purpose": "여행 목적",
                            "budget": 여행 예산,
                            "density": "여행 밀도",
                            "hotelLocation": "숙소 위치",
                            "travelMode": "이동 방식",
                            "itinerary": [
                              {
                                "date": "일자",
                                "weather": "날씨 정보",
                                "totalCost": "총 비용",
                                "schedule": [
                                  {
                                    "departureTime": "출발 시간",
                                    "category": "카테고리",
                                    "location": "장소 이동 정보 (정확한 명칭 + 지역 지점 포함(부산내 지역만))",
                                    "duration": "소요 시간 (ex: 30분, 도보)",
                                    "cost": "비용 정보 (예: 입장료 3,000원, 식사비 12,000원 등)"
                                  }
                                ]
                              }
                            ]
                          }
                        }

                        위 조건을 모두 반영하여, 사용자에게 유익하고 만족스러운 여행 계획을 유효한 JSON 형식으로 생성해 주세요.

                        """
        .formatted(
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