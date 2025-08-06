package com.example.attendance.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.attendance.model.Attendance;
import com.example.attendance.model.Holiday;
import com.example.attendance.model.User;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.EmailService;
import com.example.attendance.service.HolidayService;
import com.example.attendance.service.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@EnableScheduling
public class ScheduledHolidayRegistrationTask {

    @Autowired
    private HolidayService holidayService;
    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private UserService userService;
    @Autowired
    private EmailService emailService;

    @Scheduled(cron = "0 0 5 * * MON")
    public void registerHolidaysWeekly() {
        System.out.println("🔄 Starting holiday registration task...");
        
        // Bir sonraki haftanın başlangıç tarihini hesapla
        LocalDate nextWeekStart = calculateNextWeekStart();
        LocalDate nextWeekEnd = nextWeekStart.plusDays(4); // Cuma günü
        
        System.out.println("📅 Next week: " + nextWeekStart + " to " + nextWeekEnd);
        
        // Bir sonraki haftadaki tatil günlerini getir
        List<Holiday> holidays = holidayService.getHolidaysBetweenDates(nextWeekStart, nextWeekEnd);
        
        System.out.println("🎉 Found " + holidays.size() + " holidays in next week");
        
        // Tüm kullanıcıları getir
        List<User> allUsers = userService.getAllUsersAsEntities();
        
        // Her kullanıcı için attendance kaydını kontrol et ve güncelle
        for (User user : allUsers) {
            updateUserAttendanceForHolidays(user.getId(), nextWeekStart, holidays);
        }
        
        // Eğer tatil günleri varsa kullanıcılara email gönder
        if (!holidays.isEmpty()) {
            sendHolidayNotificationEmails(allUsers, holidays, nextWeekStart, nextWeekEnd);
        }
        
        System.out.println("✅ Holiday registration task completed for " + allUsers.size() + " users");
    }
    
    private void sendHolidayNotificationEmails(List<User> users, List<Holiday> holidays, LocalDate weekStart, LocalDate weekEnd) {
        System.out.println("📧 Sending holiday notification emails to " + users.size() + " users");
        
        // Tatil günlerini formatla
        String holidayList = formatHolidayList(holidays);
        
        // Hafta aralığını formatla
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        String weekRange = weekStart.format(formatter) + " - " + weekEnd.format(formatter);
        
        for (User user : users) {
            String email = user.getEmail();
            if (email != null && !email.trim().isEmpty()) {
                String subject = "📅 Haftalık Tatil Bildirimi";
                String body = createHolidayEmailBody(user, holidayList, weekRange);
                
                try {
                    emailService.sendEmail(email, subject, body);
                    System.out.println("📧 Holiday notification sent to: " + email);
                } catch (Exception e) {
                    System.err.println("❌ Failed to send holiday email to " + email + ": " + e.getMessage());
                }
            }
        }
    }
    
    private String formatHolidayList(List<Holiday> holidays) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        
        return holidays.stream()
            .map(holiday -> {
                String startDate = holiday.getDate().format(formatter);
                if (holiday.getEndDate() != null && !holiday.getDate().equals(holiday.getEndDate())) {
                    String endDate = holiday.getEndDate().format(formatter);
                    return "• " + holiday.getName() + " (" + startDate + " - " + endDate + ")";
                } else {
                    return "• " + holiday.getName() + " (" + startDate + ")";
                }
            })
            .collect(Collectors.joining("\n"));
    }
    
    private String createHolidayEmailBody(User user, String holidayList, String weekRange) {
        return "Merhaba " + user.getFirstName() + " " + user.getLastName() + ",\n\n" +
               "Bu hafta (" + weekRange + ") aşağıdaki resmi tatil günleri bulunmaktadır:\n\n" +
               holidayList + "\n\n" +
               "Bu günler otomatik olarak 'Resmi Tatil' olarak işaretlenmiştir.\n\n" +
               "İyi tatiller dileriz!\n\n" +
               "Saygılarımızla,\n" +
               "İnsan Kaynakları";
    }
    
    private void updateUserAttendanceForHolidays(Long userId, LocalDate weekStart, List<Holiday> holidays) {
        // Mevcut attendance kaydını al
        Attendance attendance = attendanceService.getAttendanceByUserIdAndWeekStart(userId, weekStart);
        
        if (attendance == null) {
            // Attendance kaydı yoksa yeni oluştur
            attendance = new Attendance(userId, weekStart);
        }
        
        // Haftanın her günü için tatil kontrolü yap
        for (int dayIndex = 0; dayIndex < 5; dayIndex++) {
            LocalDate currentDate = weekStart.plusDays(dayIndex);
            
            // Bu gün tatil mi kontrol et - tatil aralığının içinde mi?
            boolean isHoliday = holidays.stream()
                .anyMatch(holiday -> {
                    LocalDate holidayStart = holiday.getDate();
                    LocalDate holidayEnd = holiday.getEndDate() != null ? holiday.getEndDate() : holiday.getDate();
                    
                    // currentDate, holidayStart ile holidayEnd arasında mı? (dahil)
                    return !currentDate.isBefore(holidayStart) && !currentDate.isAfter(holidayEnd);
                });
            
            if (isHoliday) {
                // Tatil günü ise attendance'ı 5 (Resmi Tatil) olarak işaretle
                updateAttendanceDay(attendance, dayIndex, 5);
                System.out.println("🎉 Marked " + currentDate + " as holiday for user " + userId);
            }
        }
        
        // Attendance kaydını kaydet
        attendanceService.saveAttendance(attendance);
    }
    
    private void updateAttendanceDay(Attendance attendance, int dayIndex, int status) {
        switch (dayIndex) {
            case 0: // Pazartesi
                attendance.setMonday(status);
                break;
            case 1: // Salı
                attendance.setTuesday(status);
                break;
            case 2: // Çarşamba
                attendance.setWednesday(status);
                break;
            case 3: // Perşembe
                attendance.setThursday(status);
                break;
            case 4: // Cuma
                attendance.setFriday(status);
                break;
        }
    }
    
    private LocalDate calculateNextWeekStart() {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Pazartesi, 7=Pazar
        int daysUntilNextMonday = (8 - dayOfWeek) % 7; // Bir sonraki pazartesiye kaç gün var
        return today.plusDays(daysUntilNextMonday);
    }
}
