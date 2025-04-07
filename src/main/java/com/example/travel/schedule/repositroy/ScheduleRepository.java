package com.example.travel.schedule.repositroy;

import com.example.travel.schedule.model.Schedule;
import com.example.travel.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("SELECT s FROM Schedule s WHERE s.user.id = :userId")
    List<Schedule> findByUserId(@Param("userId") Long userId);

    List<Schedule> findByUser(User user);

    /**
     * 가장 최근에 생성된 일정 5개를 조회합니다.
     */
    List<Schedule> findTop5ByOrderByCreatedAtDesc();
}
