package com.example.attendance.controller;

import com.example.attendance.model.Holiday;
import com.example.attendance.service.HolidayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {
    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    // Tüm tatilleri getir
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'VIEW_HOLIDAYS'})")
    @GetMapping
    public ResponseEntity<List<Holiday>> getAllHolidays() {
        return ResponseEntity.ok(holidayService.getAllHolidays());
    }

    // Tatilleri dış API'dan çekip kaydet (örn. yıl parametresi ile)
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'CREATE_HOLIDAY'})")
    @PostMapping("/fetch")
    public ResponseEntity<List<Holiday>> fetchAndSaveHolidays(@RequestParam(required = false) Integer year) {
        int y = (year != null) ? year : Year.now().getValue();
        List<Holiday> saved = holidayService.fetchAndSaveHolidays(y);
        return ResponseEntity.ok(saved);
    }
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'VIEW_HOLIDAYS'})")
    @PostMapping("/fetch-multi")
    public ResponseEntity<String> fetchAndSaveHolidaysForNextYears(@RequestParam(defaultValue = "5") int years) {
        int startYear = java.time.Year.now().getValue();
        holidayService.fetchAndSaveHolidaysForNextYears(startYear, years);
        return ResponseEntity.ok("Tatiller başarıyla çekildi: " + startYear + " - " + (startYear + years - 1));
    }

    // Tatil ekle
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'CREATE_HOLIDAY'})")
    @PostMapping
    public ResponseEntity<Holiday> addHoliday(@RequestBody Holiday holiday) {
        Holiday saved = holidayService.addHoliday(holiday);
        return ResponseEntity.ok(saved);
    }

    // Tatil sil
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_HOLIDAYS'})")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }
} 