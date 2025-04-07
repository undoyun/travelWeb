package com.example.travel.itinerary.repository;

import com.example.travel.itinerary.model.TravelPlan;
import com.example.travel.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {
    @Query("SELECT tp FROM TravelPlan tp WHERE tp.user.id = :userId")
    List<TravelPlan> findByUserId(@Param("userId") Long userId);

    List<TravelPlan> findByUser(User user);

    long countByCreatedAtAfter(LocalDateTime dateTime);
}
