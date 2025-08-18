package com.example.attendance.repository;

import com.example.attendance.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByCountryCode(String countryCode);
    List<Holiday> findByCountryCodeAndDateBetween(String countryCode, LocalDate start, LocalDate end);
    boolean existsByCountryCodeAndDate(String countryCode, LocalDate date);
    List<Holiday> findByCountryCodeAndDate(String countryCode, LocalDate date);
} 