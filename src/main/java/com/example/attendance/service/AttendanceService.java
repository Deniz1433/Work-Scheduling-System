package com.example.attendance.service;

import com.example.attendance.model.Attendance;
import com.example.attendance.model.Excuse;
import com.example.attendance.model.User;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.ExcuseRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.dto.TeamAttendanceDto;

import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceService {
    private final AttendanceRepository repo;
    private final ExcuseRepository excuseRepo;
    private final UserRepository userRepo;

    public AttendanceService(AttendanceRepository repo, UserRepository userRepo, ExcuseRepository excuseRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.excuseRepo = excuseRepo;
    }

    
    public List<TeamAttendanceDto> getTeamAttendance(String keycloakId) {
        // 1. Kullanıcıyı keycloakId'ye göre bul
        User currentUser = userRepo.findByKeycloakId(keycloakId).orElse(null);
        if (currentUser == null) {
            return new ArrayList<>();
        }

        // 2. Kullanıcının departman ID'sini al
        Long departmentId = currentUser.getDepartment().getId();

        // 3. Aynı departmandaki tüm kullanıcıları getir
        List<User> departmentUsers = userRepo.findByDepartmentId(departmentId);

        // 4. Gelecek haftanın başlangıç tarihini hesapla
        LocalDate nextWeekStart = calculateNextWeekStart();

        // 5. Her kullanıcı için attendance verilerini al ve DTO'ya dönüştür
        return departmentUsers.stream()
                .map(user -> {
                    TeamAttendanceDto dto = new TeamAttendanceDto();
                    dto.setId(user.getId()); // Long ID
                    dto.setName(user.getFirstName());
                    dto.setSurname(user.getLastName());
                    dto.setDepartment(user.getDepartment().getName());
                    dto.setDepartmentId(user.getDepartment().getId());


                    Attendance attendance = repo.findByUserIdAndWeekStart(user.getId(), nextWeekStart);
                    if (attendance != null) {
                       
                        List<Integer> attendanceIntegers = attendance.getDates().stream()
                                .map(day -> day) 
                                .collect(Collectors.toList());
                        dto.setAttendance(attendanceIntegers);
                        dto.setApproved(attendance.isApproved());
                    } else {
                        // Attendance kaydı yoksa varsayılan değerler
                        dto.setAttendance(List.of(0,0,0,0,0));
                        dto.setApproved(false);
                    }

                    dto.setEmployeeExcuse(null); // Şimdilik null, sonra excuse tablosundan alınabilir
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<TeamAttendanceDto> getTeamAttendanceWithFilters(
            String keycloakId, 
            String departmentId, 
            String roleId, 
            String searchTerm
    ) {
        System.out.println("🔍 getTeamAttendanceWithFilters called with:");
        System.out.println("  - keycloakId: " + keycloakId);
        System.out.println("  - departmentId: " + departmentId);
        System.out.println("  - roleId: " + roleId);
        System.out.println("  - searchTerm: " + searchTerm);
        
        // 1. Kullanıcıyı keycloakId'ye göre bul
        User currentUser = userRepo.findByKeycloakId(keycloakId).orElse(null);
        if (currentUser == null) {
            System.out.println("❌ User not found for keycloakId: " + keycloakId);
            return new ArrayList<>();
        }
        
        System.out.println("✅ User found: " + currentUser.getFirstName() + " " + currentUser.getLastName());

        // 2. Filtreleme için kullanıcı listesini al
        List<User> users;
        
        // Çoklu departman filtresi
        if (departmentId != null && !departmentId.isEmpty()) {
            // Virgülle ayrılmış departman ID'lerini parse et
            String[] departmentIds = departmentId.split(",");
            if (departmentIds.length == 1) {
                // Tek departman
                users = userRepo.findByDepartmentId(Long.parseLong(departmentIds[0].trim()));
                System.out.println("🔍 Found " + users.size() + " users in department " + departmentIds[0].trim());
            } else {
                // Çoklu departman - birleşim (union) mantığı
                users = new ArrayList<>();
                for (String deptId : departmentIds) {
                    List<User> deptUsers = userRepo.findByDepartmentId(Long.parseLong(deptId.trim()));
                    users.addAll(deptUsers);
                    System.out.println("🔍 Found " + deptUsers.size() + " users in department " + deptId.trim());
                }
            }
        } else {
            // Tüm departmanlardaki kullanıcıları getir
            users = userRepo.findAll();
            System.out.println("🔍 Found " + users.size() + " total users (no department filter)");
        }

        // 3. Gelecek haftanın başlangıç tarihini hesapla
        LocalDate nextWeekStart = calculateNextWeekStart();

        // 4. Filtreleme ve DTO dönüşümü
        List<TeamAttendanceDto> result = users.stream()
                .filter(user -> {
                    // Çoklu rol filtresi - birleşim mantığı
                    if (roleId != null && !roleId.isEmpty()) {
                        String[] roleIds = roleId.split(",");
                        boolean hasMatchingRole = false;
                        for (String rId : roleIds) {
                            if (user.getRole().getId().toString().equals(rId.trim())) {
                                hasMatchingRole = true;
                                break;
                            }
                        }
                        if (!hasMatchingRole) {
                            return false;
                        }
                    }
                    
                    // Arama terimi filtresi (isim, soyisim, email)
                    if (searchTerm != null && !searchTerm.isEmpty()) {
                        String searchLower = searchTerm.toLowerCase();
                        boolean matchesSearch = (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(searchLower)) ||
                                              (user.getLastName() != null && user.getLastName().toLowerCase().contains(searchLower)) ||
                                              (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower));
                        if (!matchesSearch) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .map(user -> {
                    TeamAttendanceDto dto = new TeamAttendanceDto();
                    dto.setId(user.getId()); // Long ID
                    dto.setName(user.getFirstName());
                    dto.setSurname(user.getLastName());
                    dto.setDepartment(user.getDepartment().getName());
                    dto.setDepartmentId(user.getDepartment().getId());

                    Attendance attendance = repo.findByUserIdAndWeekStart(user.getId(), nextWeekStart);
                    if (attendance != null) {
                        List<Integer> attendanceIntegers = attendance.getDates().stream()
                                .map(day -> day) 
                                .collect(Collectors.toList());
                        dto.setAttendance(attendanceIntegers);
                        dto.setApproved(attendance.isApproved());
                    } else {
                        // Attendance kaydı yoksa varsayılan değerler
                        dto.setAttendance(List.of(0,0,0,0,0));
                        dto.setApproved(false);
                    }

                    dto.setEmployeeExcuse(null);
                    return dto;
                })
                .collect(Collectors.toList());
                
        System.out.println("✅ Returning " + result.size() + " team members");
        return result;
    }

    private LocalDate calculateNextWeekStart() {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Pazartesi, 7=Pazar
        int daysUntilNextMonday = (8 - dayOfWeek) % 7; // Bir sonraki pazartesiye kaç gün var
        return today.plusDays(daysUntilNextMonday);
    }

    /**
     * Overwrites this user's attendance for the current week (Mon–Fri)
     * by first deleting any existing rows in that range, then saving the new ones.
     */
    @Transactional    // ← ensure this method is transactional
    public void record(Long userId, LocalDate weekStart, List<Integer> dates) {
        System.out.println("Record method called with userId: " + userId + ", weekStart: " + weekStart + ", dates: " + dates);
        
        // 1) Attendance verisi var ise al
        Attendance attendance = repo.findByUserIdAndWeekStart(userId, weekStart);
        System.out.println("Existing attendance found: " + (attendance != null));

        // 2) Attendance kaydı yoksa kaydet
        if(attendance == null) {
            System.out.println("Creating new attendance record");
            Attendance newAttendance = new Attendance(userId, weekStart);
            newAttendance.setMonday(dates.get(0));
            newAttendance.setTuesday(dates.get(1));
            newAttendance.setWednesday(dates.get(2));
            newAttendance.setThursday(dates.get(3));
            newAttendance.setFriday(dates.get(4));
            newAttendance.setApproved(false);
            Attendance saved = repo.save(newAttendance);
            System.out.println("New attendance saved with ID: " + saved.getId());
        }
        // Attendance kaydı var ise güncelle
        else{
            System.out.println("Updating existing attendance record with ID: " + attendance.getId());
            attendance.setMonday(dates.get(0));
            attendance.setTuesday(dates.get(1));
            attendance.setWednesday(dates.get(2));
            attendance.setThursday(dates.get(3));
            attendance.setFriday(dates.get(4));
            attendance.setApproved(false);
            Attendance saved = repo.save(attendance); // Bu satır eksikti!
            System.out.println("Attendance updated successfully with ID: " + saved.getId());
        }
    }

    public ArrayList<Object> fetch(Long userId, LocalDate weekStart) {
        Attendance attendance = repo.findByUserIdAndWeekStart(userId, weekStart);   
        if(attendance == null) {
                return new ArrayList<>(List.of(List.of(0, 0, 0, 0, 0), false));
        }
        return new ArrayList<>(List.of(attendance.getDates(), attendance.isApproved()));

    }

    public void approve(Long id, LocalDate weekStart) {
        Attendance attendance = repo.findByUserIdAndWeekStart(id, weekStart);
        if(attendance == null){
            return;
        }
        attendance.setApproved(true);
        repo.save(attendance);
    }

    public List<Excuse> getExcuse(Long editorId, Long userId) {
        List<Excuse> excuses = excuseRepo.findByUserId(userId);
        LocalDate weekStart = calculateNextWeekStart();
        return excuses.stream().filter(e -> e.getExcuseDate().isAfter(weekStart)).collect(Collectors.toList());
    }

    public void approveExcuse(Long id, String username) {
        Excuse excuse = excuseRepo.findById(id).orElseThrow(() -> new RuntimeException("Excuse not found"));
        excuse.setIsApproved(true);
        excuseRepo.save(excuse);
    }
    
    public List<Attendance> getAllAttendance() {
        return repo.findAll();
    }
    
        public List<Attendance> getAttendanceByUserId(Long userId) {
        return repo.findByUserId(userId);
    }

    public List<Attendance> getUserAttendance(Long userId) {
        return repo.findByUserId(userId);
    }

    public Attendance getAttendanceByUserIdAndWeekStart(Long userId, LocalDate weekStart) {
        return repo.findByUserIdAndWeekStart(userId, weekStart);
    }

    public void saveAttendance(Attendance attendance) {
        repo.save(attendance);
    }
} 