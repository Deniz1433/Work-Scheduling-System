// src/main/java/com/example/attendance/service/AttendanceService.java
package com.example.attendance.service;

import com.example.attendance.model.AttendanceDay;
import com.example.attendance.model.AttendanceEntry;
import com.example.attendance.model.AttendanceWeek;
import com.example.attendance.model.Excuse;
import com.example.attendance.model.User;
import com.example.attendance.repository.AttendanceDayRepository;
import com.example.attendance.repository.AttendanceEntryRepository;
import com.example.attendance.repository.AttendanceWeekRepository;
import com.example.attendance.repository.ExcuseRepository;
import com.example.attendance.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final UserRepository userRepository;
    private final AttendanceWeekRepository weekRepository;
    private final AttendanceEntryRepository entryRepository;
    private final AttendanceDayRepository dayRepository;
    private final ExcuseRepository excuseRepository;

    @Transactional
    public List<Integer> getWeekAttendance(String keycloakId, LocalDate weekStart) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AttendanceWeek week = weekRepository.findByStartDate(weekStart).orElse(null);
        if (week == null) {
            return Arrays.asList(0, 0, 0, 0, 0);
        }

        AttendanceEntry entry = entryRepository
                .findByUserIdAndWeekId(user.getId(), week.getId())
                .orElse(null);
        if (entry == null) {
            return Arrays.asList(0, 0, 0, 0, 0);
        }

        Map<String, Integer> dayToStatus = new HashMap<>();
        dayRepository.findByAttendanceEntryId(entry.getId())
                .forEach(d -> dayToStatus.put(d.getDayOfWeek(), statusToInt(d.getStatus())));

        List<Integer> statuses = new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0));
        String[] days = {"MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"};
        for (int i = 0; i < days.length; i++) {
            statuses.set(i, dayToStatus.getOrDefault(days[i], 0));
        }

        // overlay excused days
        LocalDate weekEnd = weekStart.plusDays(4);
        List<Excuse> excuses = excuseRepository.findByUser_KeycloakId(keycloakId)
                .stream()
                .filter(exc -> {
                    LocalDate start = exc.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate end   = exc.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return !start.isAfter(weekEnd) && !end.isBefore(weekStart);
                })
                .collect(Collectors.toList());

        for (Excuse exc : excuses) {
            LocalDate start = exc.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate end   = exc.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate cursor = start.isBefore(weekStart) ? weekStart : start;
            LocalDate last   = end.isAfter(weekEnd)     ? weekEnd   : end;
            while (!cursor.isAfter(last)) {
                int idx = cursor.getDayOfWeek().getValue() - 1;
                if (idx >= 0 && idx < 5) statuses.set(idx, 4);
                cursor = cursor.plusDays(1);
            }
        }

        return statuses;
    }

    @Transactional
    public void saveOrUpdate(String keycloakId, LocalDate weekStart, List<Integer> statuses) {
        if (statuses.size() != 5) throw new IllegalArgumentException("Week must have 5 days");

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate weekEnd = weekStart.plusDays(4);
        AttendanceWeek week = weekRepository.findByStartDate(weekStart)
                .orElseGet(() -> {
                    AttendanceWeek w = new AttendanceWeek();
                    w.setStartDate(weekStart);
                    w.setEndDate(weekEnd);
                    w.setCreatedAt(new Date());
                    return weekRepository.save(w);
                });

        AttendanceEntry entry = entryRepository.findByUserIdAndWeekId(user.getId(), week.getId())
                .orElseGet(() -> {
                    AttendanceEntry e = new AttendanceEntry();
                    e.setUser(user);
                    e.setWeek(week);
                    e.setSubmittedAt(new Date());
                    return entryRepository.save(e);
                });

        dayRepository.deleteByAttendanceEntryId(entry.getId());

        DayOfWeek[] days = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        };
        for (int i = 0; i < 5; i++) {
            AttendanceDay day = new AttendanceDay();
            day.setAttendanceEntry(entry);
            day.setDayOfWeek(days[i].name());
            day.setStatus(intToStatus(statuses.get(i)));
            dayRepository.save(day);
        }
    }

    private int statusToInt(String status) {
        return switch (status) {
            case "office"  -> 1;
            case "online"  -> 2;
            case "leave"   -> 3;
            case "excuse"  -> 4;
            case "holiday" -> 5;
            default         -> 0;
        };
    }

    private String intToStatus(int i) {
        return switch (i) {
            case 1 -> "office";
            case 2 -> "online";
            case 3 -> "leave";
            case 4 -> "excuse";
            case 5 -> "holiday";
            default -> "none";
        };
    }
}
