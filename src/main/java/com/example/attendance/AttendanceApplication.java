package com.example.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.example.attendance.service.HolidayService;

@SpringBootApplication
public class AttendanceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AttendanceApplication.class, args);
	}

	@Bean
	public CommandLineRunner fetchHolidaysOnStartup(HolidayService holidayService) {
		return args -> {
			try {
				// 5 yıllık tatilleri çek (şu anki yıldan başlayarak)
				int currentYear = java.time.Year.now().getValue();
				holidayService.fetchAndSaveHolidaysForNextYears(currentYear, 5);
				System.out.println("Türkiye tatilleri başarıyla çekildi ve kaydedildi: " + currentYear + " - " + (currentYear + 4));
			} catch (Exception e) {
				System.err.println("Tatiller çekilemedi: " + e.getMessage());
			}
		};
	}
}
