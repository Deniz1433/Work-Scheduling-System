package com.example.attendance.service;

import com.example.attendance.model.Attendance;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.dto.TeamAttendanceDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;    // ← add this import

import java.time.LocalDate;
import java.util.List;

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
    public void record(String userId, LocalDate weekStart, List<Integer> dates) {
        
        // 1) Attendance verisi var ise al
        Attendance attendance = repo.findByUserIdAndWeekStart(userId, weekStart.toString());

        // 2) Attendance kaydı yoksa kaydet
        if(attendance == null) {
            Attendance newAttendance = new Attendance(userId, weekStart.toString());
            newAttendance.setMonday(dates.get(0));
            newAttendance.setTuesday(dates.get(1));
            newAttendance.setWednesday(dates.get(2));
            newAttendance.setThursday(dates.get(3));
            newAttendance.setFriday(dates.get(4));
            newAttendance.setApproved(false);
            repo.save(newAttendance);
        }
        // Attendance kaydı var ve henüz onaylanmadıysa kaydey
        else if(attendance.isApproved() == false){
            attendance.setMonday(dates.get(0));
            attendance.setTuesday(dates.get(1));
            attendance.setWednesday(dates.get(2));
            attendance.setThursday(dates.get(3));
            attendance.setFriday(dates.get(4));
        }
    }

    public List<Integer> fetch(String userId, String weekStart) {
        Attendance attendance = repo.findByUserIdAndWeekStart(userId, weekStart);   
        if(attendance == null) {
            return List.of(0, 0, 0, 0, 0);
        }
        return attendance.getDates();
    }
}
