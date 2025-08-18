package com.example.attendance.service;

import com.example.attendance.model.Attendance;
import com.example.attendance.model.Excuse;
import com.example.attendance.model.User;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.ExcuseRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.dto.TeamAttendanceDto;
import com.example.attendance.security.CustomAnnotationEvaluator;

import org.springframework.cglib.core.Local;
import org.springframework.security.core.Authentication;
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
    private final CustomAnnotationEvaluator permissionEvaluator;

    public AttendanceService(AttendanceRepository repo, UserRepository userRepo, ExcuseRepository excuseRepo, CustomAnnotationEvaluator permissionEvaluator) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.excuseRepo = excuseRepo;
        this.permissionEvaluator = permissionEvaluator;
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

    public List<TeamAttendanceDto> getTeamAttendanceWithFiltersAndPermissions(
            String keycloakId,
            Authentication authentication,
            String departmentId, 
            String roleId, 
            String searchTerm,
            String workStatus
    ) {
        System.out.println("🔍 getTeamAttendanceWithFiltersAndPermissions called with:");
        System.out.println("  - keycloakId: " + keycloakId);
        System.out.println("  - roleId: " + roleId);
        System.out.println("  - searchTerm: " + searchTerm);
        System.out.println("  - workStatus: " + workStatus);
        
        // 1. Kullanıcıyı keycloakId'ye göre bul
        User currentUser = userRepo.findByKeycloakId(keycloakId).orElse(null);
        if (currentUser == null) {
            System.out.println("❌ User not found for keycloakId: " + keycloakId);
            return new ArrayList<>();
        }
        
        System.out.println("✅ User found: " + currentUser.getFirstName() + " " + currentUser.getLastName());

        // 2. Tüm kullanıcıları al
        List<User> allUsers = userRepo.findAll();
        System.out.println("🔍 Total users in system: " + allUsers.size());

        // 3. Yetki kontrolü yaparak hangi kullanıcıları görebileceğini belirle
        List<User> authorizedUsers = allUsers.stream()
                .filter(user -> permissionEvaluator.canViewAttendance(authentication, user.getId()))
                .collect(Collectors.toList());
        
        System.out.println("🔍 Users after permission check: " + authorizedUsers.size());

        // 4. Filtreleri uygula
        List<User> filteredUsers = authorizedUsers.stream()
                .filter(user -> {
                    // Departman filtresi
                    if (departmentId != null && !departmentId.isEmpty()) {
                        String[] deptIds = departmentId.split(",");
                        boolean hasMatchingDept = false;
                        for (String deptId : deptIds) {
                            if (user.getDepartment() != null && user.getDepartment().getId().toString().equals(deptId.trim())) {
                                hasMatchingDept = true;
                                break;
                            }
                        }
                        if (!hasMatchingDept) return false;
                    }
                    
                    // Rol filtresi
                    if (roleId != null && !roleId.isEmpty()) {
                        String[] roleIds = roleId.split(",");
                        boolean hasMatchingRole = false;
                        for (String rId : roleIds) {
                            if (user.getRole() != null && user.getRole().getId().toString().equals(rId.trim())) {
                                hasMatchingRole = true;
                                break;
                            }
                        }
                        if (!hasMatchingRole) return false;
                    }
                    
                    // Arama filtresi
                    if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                        String search = searchTerm.toLowerCase().trim();
                        String fullName = (user.getFirstName() + " " + user.getLastName()).toLowerCase();
                        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                        
                        if (!fullName.contains(search) && !email.contains(search)) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());

        System.out.println("🔍 Users after filtering: " + filteredUsers.size());

        // 5. Gelecek haftanın başlangıç tarihini hesapla
        LocalDate nextWeekStart = calculateNextWeekStart();

        // 6. Her kullanıcı için attendance verilerini al ve DTO'ya dönüştür
        List<TeamAttendanceDto> result = filteredUsers.stream()
                .map(user -> {
                    TeamAttendanceDto dto = new TeamAttendanceDto();
                    dto.setId(user.getId());
                    dto.setName(user.getFirstName());
                    dto.setSurname(user.getLastName());
                    dto.setDepartment(user.getDepartment() != null ? user.getDepartment().getName() : "N/A");
                    dto.setDepartmentId(user.getDepartment() != null ? user.getDepartment().getId() : null);

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

        // 7. WorkStatus filtresini uygula (eğer belirtilmişse)
        if (workStatus != null && !workStatus.trim().isEmpty()) {
            String[] statuses = workStatus.split(",");
            System.out.println("🔍 Applying workStatus filter for: " + String.join(", ", statuses));
            System.out.println("🔍 Total users before workStatus filtering: " + result.size());
            
            result = result.stream()
                .filter(dto -> {
                    if (dto.getAttendance() == null || dto.getAttendance().isEmpty()) {
                        System.out.println("🔍 Filtering out user " + dto.getName() + " - no attendance data");
                        return false;
                    }
                    
                    System.out.println("🔍 Checking user " + dto.getName() + " with attendance: " + dto.getAttendance());
                    
                    // Kullanıcının hafta içinde en az bir gününde belirtilen durumda olup olmadığını kontrol et
                    boolean hasMatchingStatus = false;
                    for (Integer status : dto.getAttendance()) {
                        if (status != null) { // 0 = veri yok, 1-5 = çeşitli durumlar
                            for (String requestedStatus : statuses) {
                                if (status.toString().equals(requestedStatus.trim())) {
                                    System.out.println("🔍 User " + dto.getName() + " matches status " + status + " (requested: " + requestedStatus + ")");
                                    hasMatchingStatus = true;
                                    break;
                                }
                            }
                            if (hasMatchingStatus) break;
                        }
                    }
                    
                    if (!hasMatchingStatus) {
                        System.out.println("🔍 Filtering out user " + dto.getName() + " - no matching status. Attendance: " + dto.getAttendance());
                    }
                    
                    return hasMatchingStatus;
                })
                .collect(Collectors.toList());
            
            System.out.println("🔍 Users after workStatus filtering: " + result.size());
        } else {
            System.out.println("🔍 No workStatus filter applied, showing all users");
        }
                
        System.out.println("✅ Returning " + result.size() + " team members with permissions");
        return result;
    }

    private LocalDate calculateNextWeekStart() {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Pazartesi, 7=Pazar
        int daysUntilNextMonday = (8 - dayOfWeek) % 7; // Bir sonraki pazartesiye kaç gün var
        // Pazartesi günü isek (0 gün), frontend'in gösterdiği gibi daima bir SONRAKİ haftayı alalım
        if (daysUntilNextMonday == 0) {
            daysUntilNextMonday = 7;
        }
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
    
    public Excuse getExcuseById(Long excuseId) {
        return excuseRepo.findById(excuseId).orElse(null);
    }
    
    public List<Attendance> getAllAttendance() {
        return repo.findAll();
    }
    
    public List<Attendance> getAttendanceByUserId(Long userId) {
        return repo.findByUserId(userId);
    }
    
    public Attendance getAttendanceByUserIdAndWeekStart(Long userId, LocalDate weekStart) {
        return repo.findByUserIdAndWeekStart(userId, weekStart);
    }
    
    public void saveAttendance(Attendance attendance) {
        repo.save(attendance);
    }
} 