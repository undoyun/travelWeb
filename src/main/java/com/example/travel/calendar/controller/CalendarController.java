package com.example.travel.calendar.controller;

import com.example.travel.calendar.model.CalendarDTO;
import com.example.travel.calendar.service.CalendarService;
import com.example.travel.schedule.model.Schedule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*") // CORS 문제 해결
public class CalendarController {

    private final CalendarService eventService;

    public CalendarController(CalendarService eventService) {
        this.eventService = eventService;
    }

    // 📌 캘린더에서 일정 조회 API
    @GetMapping
    public ResponseEntity<?> getEvents(Authentication authentication) {
        try {
            // 인증 객체가 없는 경우(로그인하지 않은 경우) 빈 배열 반환
            if (authentication == null) {
                System.out.println("인증되지 않은 사용자: 빈 일정 목록 반환");
                return ResponseEntity.ok(List.of());
            }

            String userId = authentication.getName();
            List<CalendarDTO> events = eventService.getAllEvents(userId);

            // 디버깅 정보 추가
            System.out.println("사용자 ID: " + userId + "의 일정 조회 - 총 " + events.size() + "개 일정");

            return ResponseEntity.ok(events);
        } catch (Exception e) {
            // 에러 처리 및 로깅
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "일정을 불러오는 중 오류가 발생했습니다");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 📌 일정 제목 수정 API
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable(name = "id") Long id,
            @RequestBody Map<String, String> updateData) {
        try {
            String newPlanName = updateData.get("planName");
            String newPlanDescription = updateData.get("planDescription");

            if (newPlanName == null || newPlanName.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "제목이 비어 있습니다."));
            }

            // null 체크는 하되 빈 문자열은 허용
            if (newPlanDescription == null) {
                newPlanDescription = "";
            }

            // 일정 업데이트 후 DTO로 반환하여 무한 재귀 방지
            Schedule updatedSchedule = eventService.updateSchedule(id, newPlanName, newPlanDescription);

            // 응답을 간소화된 형태로 변환
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedSchedule.getId());
            response.put("planName", updatedSchedule.getPlanName());
            response.put("planDescription", updatedSchedule.getPlanDescription());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "일정 업데이트 중 오류가 발생했습니다", "message", e.getMessage()));
        }
    }

    // 📌 일정 삭제 API
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable(name = "id") Long id) {
        eventService.deleteSchedule(id);
        return ResponseEntity.ok("일정이 삭제되었습니다.");
    }

    // ✅ 정상적으로 동작하도록 GET 요청 허용
    @GetMapping("/debug")
    public ResponseEntity<List<CalendarDTO>> debugEvents(Authentication authentication) {
        // 인증 객체가 없는 경우 빈 배열 반환
        if (authentication == null) {
            System.out.println("인증되지 않은 사용자: 디버그 모드 - 빈 일정 목록 반환");
            return ResponseEntity.ok(List.of());
        }

        String userId = authentication.getName();
        List<CalendarDTO> events = eventService.getAllEvents(userId);

        // ✅ 디버깅 로그 추가
        System.out.println("사용자 ID: " + userId + "의 일정 디버깅 - 총 " + events.size() + "개 일정");
        events.forEach(event -> System.out.println("🛠 이벤트 JSON 응답: " + event));

        return ResponseEntity.ok(events);
    }
}
