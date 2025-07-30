package com.example.attendance.repository;

import com.example.attendance.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByCountryCode(String countryCode);
    List<Holiday> findByCountryCodeAndDateBetween(String countryCode, String start, String end);
    boolean existsByCountryCodeAndDate(String countryCode, String date);
} 