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
				holidayService.fetchAndSaveHolidays(java.time.Year.now().getValue());
				System.out.println("Türkiye tatilleri başarıyla çekildi ve kaydedildi.");
			} catch (Exception e) {
				System.err.println("Tatiller çekilemedi: " + e.getMessage());
			}
		};
	}
}
