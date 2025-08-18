package com.example.attendance.service;

import com.example.attendance.dto.HolidayDto;
import com.example.attendance.model.Holiday;
import com.example.attendance.repository.HolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HolidayService {
    private final HolidayRepository holidayRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String COUNTRY_CODE = "TR";
    private static final String NAGER_URL = "https://date.nager.at/api/v3/PublicHolidays/%d/%s";

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    @Transactional
    public List<Holiday> fetchAndSaveHolidays(int year) {
        String url = String.format(NAGER_URL, year, COUNTRY_CODE);
        HolidayDto[] holidays = restTemplate.getForObject(url, HolidayDto[].class);
        if (holidays == null) return List.of();
        List<Holiday> holidayEntities = Arrays.stream(holidays)
                .filter(h -> !holidayRepository.existsByCountryCodeAndDate(COUNTRY_CODE, LocalDate.parse(h.getDate())))
                .map(h -> new Holiday(null, h.getLocalName(), LocalDate.parse(h.getDate()), LocalDate.parse(h.getDate()), COUNTRY_CODE))
                .collect(Collectors.toList());
        return holidayRepository.saveAll(holidayEntities);
    }

    @Transactional
    public void fetchAndSaveHolidaysForNextYears(int startYear, int years) {
        for (int y = startYear; y < startYear + years; y++) {
            fetchAndSaveHolidays(y);
        }
    }

    @Transactional
    public Holiday addHoliday(Holiday holiday) {
        return holidayRepository.save(holiday);
    }

    @Transactional
    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
    }

    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAll();
    }

    public List<Holiday> getHolidaysBetweenDates(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findByCountryCodeAndDateBetween("TR", startDate, endDate);
    }

    // Belirli bir tarih için tatil kontrolü
    public List<Holiday> getHolidaysForDate(LocalDate date) {
        // Hem date hem de endDate aralığında olan tatilleri bul
        List<Holiday> holidays = holidayRepository.findByCountryCode("TR");
        return holidays.stream()
                .filter(holiday -> {
                    LocalDate startDate = holiday.getDate();
                    LocalDate endDate = holiday.getEndDate() != null ? holiday.getEndDate() : startDate;
                    return !date.isBefore(startDate) && !date.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }
} 