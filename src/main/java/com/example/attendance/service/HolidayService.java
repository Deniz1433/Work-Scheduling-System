package com.example.attendance.service;

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
                .filter(h -> !holidayRepository.existsByCountryCodeAndDate(COUNTRY_CODE, LocalDate.parse(h.date)))
                .map(h -> new Holiday(null, h.localName, LocalDate.parse(h.date), LocalDate.parse(h.date), COUNTRY_CODE))
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

    // DTO for Nager.Date API
    private static class HolidayDto {
        public String date;
        public String localName;
        public String name;
        public String countryCode;
    }
} 