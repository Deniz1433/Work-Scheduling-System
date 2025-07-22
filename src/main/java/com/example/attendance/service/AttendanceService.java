package com.example.attendance.service;

import com.example.attendance.model.Attendance;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.dto.TeamAttendanceDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;    // ← add this import

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceService {
    private final AttendanceRepository repo;

    public AttendanceService(AttendanceRepository repo) {
        this.repo = repo;
    }

    public List<TeamAttendanceDto> getTeamAttendance(String username) {
        TeamAttendanceDto member1 = new TeamAttendanceDto();
        member1.setId(1L);
        member1.setName("Ali");
        member1.setSurname("Yılmaz");
        member1.setDepartment("Yazılım");
        member1.setAttendance(List.of(true, true, false, true, false));
        member1.setApproved(true);
        member1.setEmployeeExcuse("Raporlu");

        TeamAttendanceDto member2 = new TeamAttendanceDto();
        member2.setId(2L);
        member2.setName("Ayşe");
        member2.setSurname("Demir");
        member2.setDepartment("Tasarım");
        member2.setAttendance(List.of(true, false, true, true, true));
        member2.setApproved(false);
        member2.setEmployeeExcuse(null);

        return List.of(member1, member2);
    }

    /**
     * Overwrites this user's attendance for the current week (Mon–Fri)
     * by first deleting any existing rows in that range, then saving the new ones.
     */
    @Transactional    // ← ensure this method is transactional
    public void record(String userId, List<LocalDate> dates) {
        // 1) Determine this week's Monday and Friday
        LocalDate reference = dates.isEmpty() ? LocalDate.now() : dates.get(0);
        LocalDate monday = reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = monday.plusDays(4);

        // 2) Delete any of this user's existing rows in that week
        repo.deleteByUserIdAndAttendanceDateBetween(userId, monday, friday);

        // 3) Save only the newly selected dates
        if (!dates.isEmpty()) {
            List<Attendance> records = dates.stream()
                    .map(d -> new Attendance(userId, d))
                    .collect(Collectors.toList());
            repo.saveAll(records);
        }
    }

    public List<LocalDate> fetch(String userId) {
        return repo.findByUserId(userId)
                .stream()
                .map(Attendance::getAttendanceDate)
                .collect(Collectors.toList());
    }
}
