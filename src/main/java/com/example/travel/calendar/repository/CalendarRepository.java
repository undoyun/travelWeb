package com.example.travel.calendar.repository;

import com.example.travel.calendar.model.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalendarRepository extends JpaRepository<Calendar, Long> {
    List<Calendar> findByDate(String date);
}
