package com.example.travel.schedule.service;

import com.example.travel.admin.dto.ScheduleSummaryDTO;
import com.example.travel.exception.ResourceNotFoundException;
import com.example.travel.schedule.dto.ScheduleDTO;
import com.example.travel.schedule.model.Schedule;
import com.example.travel.schedule.repositroy.ScheduleRepository;
import com.example.travel.user.model.User;
import com.example.travel.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 사용자의 모든 일정을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 사용자의 모든 일정 목록
     */
    @Transactional(readOnly = true)
    public List<ScheduleDTO> getSchedulesByUserId(String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            return scheduleRepository.findByUserId(userIdLong).stream()
                    .map(ScheduleDTO::fromEntity)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 username으로 간주하고 해당 사용자의 ID로 조회
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자명의 사용자가 존재하지 않습니다: " + userId));
            return scheduleRepository.findByUserId(user.getId()).stream()
                    .map(ScheduleDTO::fromEntity)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 특정 ID의 일정을 조회합니다.
     *
     * @param scheduleId 일정 ID
     * @return 조회된 일정
     * @throws ResourceNotFoundException 일정을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public ScheduleDTO getSchedule(Long scheduleId) {
        log.debug("일정 ID {}의 상세 정보 조회", scheduleId);
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));
        return ScheduleDTO.fromEntity(schedule);
    }

    /**
     * 특정 사용자의 특정 일정을 조회합니다. (권한 확인 포함)
     * 
     * @param scheduleId 일정 ID
     * @param userId     사용자 ID
     * @return 조회된 일정
     * @throws ResourceNotFoundException 일정을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public ScheduleDTO getScheduleWithAuth(Long scheduleId, String userId) {
        log.debug("사용자 ID {}의 일정 ID {} 조회", userId, scheduleId);
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        // 권한 확인 방식 개선
        User currentUser;
        try {
            // 숫자 ID로 변환 시도
            Long userIdLong = Long.parseLong(userId);
            // ID로 사용자 조회
            currentUser = userRepository.findById(userIdLong)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userIdLong));
        } catch (NumberFormatException e) {
            // 숫자가 아닌 경우 username으로 간주
            currentUser = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", userId));
        }

        // 사용자 ID로 권한 확인
        if (schedule.getUser() == null || !schedule.getUser().getId().equals(currentUser.getId())) {
            log.warn("사용자 {}(ID:{})가 다른 사용자의 일정 ID {}에 접근 시도", 
                    currentUser.getUsername(), currentUser.getId(), scheduleId);
            throw new ResourceNotFoundException("Schedule", "id", scheduleId);
        }

        return ScheduleDTO.fromEntity(schedule);
    }

    /**
     * 일정을 삭제합니다.
     * 
     * @param scheduleId 삭제할 일정 ID
     * @param userId     사용자 ID (권한 확인용)
     * @throws ResourceNotFoundException 일정을 찾을 수 없는 경우
     */
    @Transactional
    public void deleteSchedule(Long scheduleId, String userId) {
        log.debug("사용자 ID {}의 일정 ID {} 삭제", userId, scheduleId);
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        // 권한 확인 방식 개선
        User currentUser;
        try {
            // 숫자 ID로 변환 시도
            Long userIdLong = Long.parseLong(userId);
            // ID로 사용자 조회
            currentUser = userRepository.findById(userIdLong)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userIdLong));
        } catch (NumberFormatException e) {
            // 숫자가 아닌 경우 username으로 간주
            currentUser = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", userId));
        }

        // 사용자 ID로 권한 확인
        if (schedule.getUser() == null || !schedule.getUser().getId().equals(currentUser.getId())) {
            log.warn("사용자 {}(ID:{})가 다른 사용자의 일정 ID {}를 삭제 시도", 
                    currentUser.getUsername(), currentUser.getId(), scheduleId);
            throw new ResourceNotFoundException("Schedule", "id", scheduleId);
        }

        scheduleRepository.delete(schedule);
        log.info("일정 ID {} 삭제 완료", scheduleId);
    }

    /**
     * 일정을 업데이트합니다.
     * 
     * @param scheduleId      업데이트할 일정 ID
     * @param userId          사용자 ID (권한 확인용)
     * @param editedItinerary 수정된 일정 JSON
     * @param planName        여행 이름
     * @param planPhoto       여행 사진 URL
     * @param planDescription 여행 설명
     * @return 업데이트된 일정 정보
     * @throws ResourceNotFoundException 일정을 찾을 수 없는 경우
     */
    @Transactional
    public ScheduleDTO updateSchedule(Long scheduleId, String userId, String editedItinerary,
            String planName, String planPhoto, String planDescription) {
        log.debug("사용자 ID {}의 일정 ID {} 업데이트", userId, scheduleId);
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        // 권한 확인 방식 개선
        User currentUser;
        try {
            // 숫자 ID로 변환 시도
            Long userIdLong = Long.parseLong(userId);
            // ID로 사용자 조회
            currentUser = userRepository.findById(userIdLong)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userIdLong));
        } catch (NumberFormatException e) {
            // 숫자가 아닌 경우 username으로 간주
            currentUser = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", userId));
        }

        // 사용자 ID로 권한 확인
        if (schedule.getUser() == null || !schedule.getUser().getId().equals(currentUser.getId())) {
            log.warn("사용자 {}(ID:{})가 다른 사용자의 일정 ID {}를 수정 시도", 
                    currentUser.getUsername(), currentUser.getId(), scheduleId);
            throw new ResourceNotFoundException("Schedule", "id", scheduleId);
        }

        // 일정 정보 업데이트
        schedule.setScheduleJson(editedItinerary);
        schedule.setPlanName(planName);
        schedule.setPlanPhoto(planPhoto != null ? planPhoto : schedule.getPlanPhoto());
        schedule.setPlanDescription(planDescription != null ? planDescription : schedule.getPlanDescription());

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        log.info("일정 ID {} 업데이트 완료", scheduleId);

        return ScheduleDTO.fromEntity(updatedSchedule);
    }

    /**
     * 전체 일정 수를 조회합니다.
     * @return 전체 일정 수
     */
    public long getTotalSchedulesCount() {
        return scheduleRepository.count();
    }

    /**
     * 최근 등록된 일정 목록을 반환합니다.
     * @param limit 조회할 일정 수
     * @return 최근 등록된 일정 목록
     */
    public List<ScheduleSummaryDTO> getRecentSchedules(int limit) {
        List<Schedule> schedules = scheduleRepository.findTop5ByOrderByCreatedAtDesc();
        return schedules.stream()
                .map(schedule -> {
                    String userName = schedule.getUser() != null ? schedule.getUser().getUsername() : "알 수 없음";
                    
                    LocalDate startDate = LocalDate.parse(schedule.getStartDate());
                    LocalDate endDate = LocalDate.parse(schedule.getEndDate());
                    int duration = Period.between(startDate, endDate).getDays() + 1;
                    
                    return ScheduleSummaryDTO.builder()
                            .id(schedule.getId())
                            .title(schedule.getPlanName())
                            .userName(userName)
                            .duration(duration)
                            .createdAt(schedule.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
