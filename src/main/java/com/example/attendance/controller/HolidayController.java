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

    // Belirli bir tarihin tatil olup olmadığını kontrol et
    @GetMapping("/check")
    public ResponseEntity<Object> checkHoliday(@RequestParam String date) {
        try {
            System.out.println("=== HOLIDAY CHECK DEBUG ===");
            System.out.println("Requested date: " + date);
            
            java.time.LocalDate localDate = java.time.LocalDate.parse(date);
            System.out.println("Parsed LocalDate: " + localDate);
            
            // Tüm tatilleri kontrol et
            List<Holiday> allHolidays = holidayService.getAllHolidays();
            System.out.println("Total holidays in database: " + allHolidays.size());
            for (Holiday holiday : allHolidays) {
                System.out.println("Holiday: " + holiday.getName() + " - " + holiday.getDate() + " to " + holiday.getEndDate());
            }
            
            // Belirli tarih için tatil kontrolü
            List<Holiday> holidaysForDate = holidayService.getHolidaysForDate(localDate);
            System.out.println("Holidays for date " + localDate + ": " + holidaysForDate.size());
            for (Holiday holiday : holidaysForDate) {
                System.out.println("Found holiday: " + holiday.getName() + " - " + holiday.getDate() + " to " + holiday.getEndDate());
            }
            
            boolean isHoliday = holidaysForDate.size() > 0;
            System.out.println("Is holiday: " + isHoliday);
            System.out.println("=== END HOLIDAY CHECK DEBUG ===");
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("isHoliday", isHoliday);
            response.put("date", date);
            response.put("totalHolidays", allHolidays.size());
            response.put("holidaysForDate", holidaysForDate.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Holiday check error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Geçersiz tarih formatı: " + e.getMessage());
        }
    }
} 