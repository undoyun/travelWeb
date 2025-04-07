package com.example.travel.calendar.service;

import com.example.travel.calendar.model.CalendarDTO;
import com.example.travel.schedule.model.Schedule;
import com.example.travel.schedule.repositroy.ScheduleRepository;
import com.example.travel.user.model.User;
import com.example.travel.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private UserRepository userRepository;

    // 📌 스케줄 데이터를 CalendarDTO로 변환하여 반환
    public List<CalendarDTO> getAllEvents(String userId) {
        try {
            // 사용자 ID가 숫자인지 확인
            Long userIdLong = null;
            try {
                userIdLong = Long.parseLong(userId);
            } catch (NumberFormatException e) {
                // 사용자 ID가 숫자가 아닌 경우 - 사용자 이름으로 간주
                System.out.println("사용자 이름으로 간주하여 사용자 조회: " + userId);
                User user = userRepository.findByUsername(userId)
                        .orElse(null);
                
                if (user != null) {
                    System.out.println("사용자를 찾았습니다. 사용자 ID: " + user.getId());
                    List<Schedule> schedules = scheduleRepository.findByUserId(user.getId());
                    List<CalendarDTO> result = schedules.stream().map(CalendarDTO::new).collect(Collectors.toList());
                    System.out.println("사용자 " + userId + "의 일정 조회 결과: " + result.size() + "개 일정 반환");
                    return result;
                } else {
                    System.out.println("해당 사용자 이름의 사용자를 찾을 수 없습니다: " + userId);
                    return new ArrayList<>();
                }
            }

            // 사용자 ID로 일정 조회 - 현재 로그인한 사용자의 일정만 표시
            List<Schedule> schedules = scheduleRepository.findByUserId(userIdLong);
            List<CalendarDTO> result = schedules.stream().map(CalendarDTO::new).collect(Collectors.toList());

            System.out.println("사용자 ID: " + userIdLong + "의 일정 조회 결과: " + result.size() + "개 일정 반환");

            return result;
        } catch (Exception e) {
            System.err.println("일정 조회 중 오류: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // 오류 발생 시 빈 목록 반환
        }
    }

    // 📌 일정 제목 수정 기능
    @Transactional
    public Schedule updateSchedule(Long scheduleId, String newPlanName, String newPlanDescription) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다. ID: " + scheduleId));

        schedule.setPlanName(newPlanName);
        schedule.setPlanDescription(newPlanDescription);

        return scheduleRepository.save(schedule);
    }

    // 📌 일정 삭제 기능
    @Transactional
    public void deleteSchedule(Long scheduleId) {
        scheduleRepository.deleteById(scheduleId);
    }
}
