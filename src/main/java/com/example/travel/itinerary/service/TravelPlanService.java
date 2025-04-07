package com.example.travel.itinerary.service;

import com.example.travel.exception.BadRequestException;
import com.example.travel.exception.ResourceNotFoundException;
import com.example.travel.itinerary.dto.TravelPlanDTO;
import com.example.travel.itinerary.model.TravelPlan;
import com.example.travel.itinerary.repository.TravelPlanRepository;
import com.example.travel.schedule.dto.ScheduleDTO;
import com.example.travel.schedule.model.Schedule;
import com.example.travel.schedule.repositroy.ScheduleRepository;
import com.example.travel.user.model.User;
import com.example.travel.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TravelPlanService {

    private static final Logger log = LoggerFactory.getLogger(TravelPlanService.class);

    @Autowired
    private TravelPlanRepository travelPlanRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GPTService gptService;

    /**
     * 사용자의 모든 여행 계획을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<TravelPlanDTO> getUserPlans(String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            List<TravelPlan> plans = travelPlanRepository.findByUserId(userIdLong);
            return plans.stream()
                    .map(TravelPlanDTO::fromEntity)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우(예: "user1") username으로 간주
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", userId));
            List<TravelPlan> plans = travelPlanRepository.findByUser(user);
            return plans.stream()
                    .map(TravelPlanDTO::fromEntity)
                    .collect(Collectors.toList());
        }
    }

    /**
     * (1) 사용자 입력 + GPT 생성 → TravelPlan 저장
     */
    @Transactional
    public TravelPlanDTO createPlan(TravelPlanDTO planDTO) {
        validateTravelPlanInput(planDTO);

        // GPT로 일정 JSON 생성
        TravelPlan plan = planDTO.toEntity();
        
        // User 정보 처리
        if (plan.getUser() != null) {
            try {
                // ID로 사용자 조회
                if (plan.getUser().getId() != null && plan.getUser().getId() > 0) {
                    User dbUser = userRepository.findById(plan.getUser().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", plan.getUser().getId()));
                    plan.setUser(dbUser);
                } 
                // Username으로 사용자 조회
                else if (plan.getUser().getUsername() != null && !plan.getUser().getUsername().isEmpty()) {
                    User dbUser = userRepository.findByUsername(plan.getUser().getUsername())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "username", plan.getUser().getUsername()));
                    plan.setUser(dbUser);
                }
            } catch (Exception e) {
                throw new BadRequestException("사용자 정보를 처리하는 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
        
        try {
            String planContent = gptService.generatePlanContent(plan);

            // JSON 형식 검증 추가
            try {
                // JSON 형식 검증
                new ObjectMapper().readTree(planContent);
            } catch (Exception e) {
                throw new BadRequestException("GPT가 생성한 일정이 올바른 JSON 형식이 아닙니다: " + e.getMessage());
            }

            plan.setItinerary(planContent);
        } catch (Exception e) {
            throw new BadRequestException("GPT 일정 생성 중 오류가 발생했습니다: " + e.getMessage());
        }

        // TravelPlan 저장
        TravelPlan saved = travelPlanRepository.save(plan);
        return TravelPlanDTO.fromEntity(saved);
    }

    /**
     * (2) planId로 TravelPlan 조회
     */
    @Transactional(readOnly = true)
    public TravelPlanDTO getPlan(Long planId) {
        TravelPlan travelPlan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("TravelPlan", "planId", planId));
        return TravelPlanDTO.fromEntity(travelPlan);
    }

    /**
     * (3) 편집 완료 후 최종 확정 → Schedule 생성
     */
    @Transactional
    public ScheduleDTO confirmPlan(Long planId, String editedItinerary, String userId,
            String planName, String planPhoto, String planDescription) {
        if (editedItinerary == null || editedItinerary.trim().isEmpty()) {
            throw new BadRequestException("편집된 일정 정보가 없습니다.");
        }

        if (planName == null || planName.trim().isEmpty()) {
            throw new BadRequestException("여행 이름은 필수 입력 항목입니다.");
        }

        TravelPlan travelPlan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("TravelPlan", "planId", planId));

        // 권한 확인 로직 개선
        log.info("확인 - 요청 사용자 ID: {}, 여행 계획 소유자: {}", userId, 
                 travelPlan.getUser() != null ? travelPlan.getUser().getId() : "없음");
                 
        // 테스트 및 개발 환경에서는 권한 검사를 우회할 수 있도록 설정
        boolean skipAuthCheck = false;
        
        // 개발 환경일 경우 권한 검사 우회 (환경 변수 또는 프로필 기반)
        String activeProfile = System.getProperty("spring.profiles.active");
        if (activeProfile != null && (activeProfile.equals("dev") || activeProfile.equals("test"))) {
            skipAuthCheck = true;
            log.warn("개발 모드: 여행 계획 권한 검사 우회");
        }
        
        // 실제 권한 확인 로직
        if (!skipAuthCheck && (travelPlan.getUser() == null || 
                (travelPlan.getUser().getId() != null && 
                 !travelPlan.getUser().getId().toString().equals(userId) && 
                 !userId.equals(travelPlan.getUser().getUsername())))) {
            log.warn("여행 계획({})에 대한 권한 거부 - 요청자: {}, 소유자: {}/{}", 
                      planId, userId, 
                      travelPlan.getUser() != null ? travelPlan.getUser().getId() : "없음",
                      travelPlan.getUser() != null ? travelPlan.getUser().getUsername() : "없음");
            throw new BadRequestException("해당 여행 계획에 대한 권한이 없습니다.");
        }

        // 이미 완료된 계획인지 확인
        if (travelPlan.isCompleted()) {
            throw new BadRequestException("이미 확정된 여행 계획입니다.");
        }

        Schedule schedule = createScheduleFromTravelPlan(travelPlan, userId, editedItinerary);

        // 추가 정보를 Schedule에 반영
        schedule.setPlanName(planName);
        schedule.setPlanPhoto(planPhoto != null ? planPhoto : "");
        schedule.setPlanDescription(planDescription != null ? planDescription : "");

        // TravelPlan은 완료 상태로 변경
        travelPlan.setCompleted(true);
        travelPlanRepository.save(travelPlan);

        // Schedule 저장
        log.info("일정 저장 - scheduleJson: {}", schedule.getScheduleJson().substring(0, Math.min(100, schedule.getScheduleJson().length())) + "...");
        Schedule savedSchedule = scheduleRepository.save(schedule);
        
        return ScheduleDTO.fromEntity(savedSchedule);
    }

    /**
     * (4) 모달에서 받은 추가 정보를 Schedule에 업데이트
     */
    @Transactional
    public ScheduleDTO updateAdditionalInfo(Long scheduleId, String planName, String planPhoto,
            String planDescription) {
        if (planName == null || planName.trim().isEmpty()) {
            throw new BadRequestException("여행 이름은 필수 입력 항목입니다.");
        }

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        schedule.setPlanName(planName);
        schedule.setPlanPhoto(planPhoto != null ? planPhoto : schedule.getPlanPhoto());
        schedule.setPlanDescription(planDescription != null ? planDescription : schedule.getPlanDescription());

        Schedule updated = scheduleRepository.save(schedule);
        return ScheduleDTO.fromEntity(updated);
    }

    /**
     * 사용자의 모든 확정된 일정을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<ScheduleDTO> getUserSchedules(String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            List<Schedule> schedules = scheduleRepository.findByUserId(userIdLong);
            return schedules.stream()
                    .map(ScheduleDTO::fromEntity)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 username으로 찾기
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", userId));
            List<Schedule> schedules = scheduleRepository.findByUser(user);
            return schedules.stream()
                    .map(ScheduleDTO::fromEntity)
                    .collect(Collectors.toList());
        }
    }

    /**
     * TravelPlan 엔티티에서 Schedule 엔티티를 생성합니다.
     */
    private Schedule createScheduleFromTravelPlan(TravelPlan travelPlan, String userId, String editedItinerary) {
        Schedule schedule = new Schedule();

        // User 객체 설정
        try {
            Long userIdLong = Long.parseLong(userId);
            User user = userRepository.findById(userIdLong)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userIdLong));
            schedule.setUser(user);
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 username으로 찾기
            User user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", userId));
            schedule.setUser(user);
        }

        schedule.setDepartLocation(travelPlan.getDepartLocation());
        schedule.setStartDate(travelPlan.getStartDate());
        schedule.setEndDate(travelPlan.getEndDate());
        schedule.setDepartureTime(travelPlan.getDepartureTime());
        schedule.setReturnTime(travelPlan.getReturnTime());
        schedule.setTransportation(travelPlan.getTransportation());
        schedule.setDestination(travelPlan.getDestination());
        schedule.setPurpose(travelPlan.getPurpose());
        schedule.setBudget(travelPlan.getBudget());
        schedule.setDensity(travelPlan.getDensity());
        schedule.setHotelLocation(travelPlan.getHotelLocation());
        schedule.setTravelMode(travelPlan.getTravelMode());
        schedule.setScheduleJson(editedItinerary);
        return schedule;
    }

    /**
     * 여행 계획 입력값을 검증합니다.
     */
    private void validateTravelPlanInput(TravelPlanDTO planDTO) {
        // 필수 필드 검증
        if (planDTO.getDepartLocation() == null || planDTO.getDepartLocation().trim().isEmpty()) {
            throw new BadRequestException("출발지는 필수 입력 항목입니다.");
        }

        if (planDTO.getDestination() == null || planDTO.getDestination().trim().isEmpty()) {
            throw new BadRequestException("목적지는 필수 입력 항목입니다.");
        }

        if (planDTO.getStartDate() == null || planDTO.getStartDate().trim().isEmpty()) {
            throw new BadRequestException("출발 날짜는 필수 입력 항목입니다.");
        }

        if (planDTO.getEndDate() == null || planDTO.getEndDate().trim().isEmpty()) {
            throw new BadRequestException("종료 날짜는 필수 입력 항목입니다.");
        }

        // 날짜 형식 및 유효성 검증
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startDate = LocalDate.parse(planDTO.getStartDate(), formatter);
            LocalDate endDate = LocalDate.parse(planDTO.getEndDate(), formatter);

            if (startDate.isAfter(endDate)) {
                throw new BadRequestException("출발 날짜는 종료 날짜보다 이전이어야 합니다.");
            }

            if (startDate.isBefore(LocalDate.now())) {
                throw new BadRequestException("출발 날짜는 오늘 이후여야 합니다.");
            }
        } catch (DateTimeParseException e) {
            throw new BadRequestException("날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요.");
        }

        // 예산 검증
        if (planDTO.getBudget() < 0) {
            throw new BadRequestException("예산은 0 이상이어야 합니다.");
        }
    }
}
