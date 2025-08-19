// src/main/java/com/example/attendance/tasks/PermissionSeeder.java
package com.example.attendance.tasks;

import com.example.attendance.model.Permission;
import com.example.attendance.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionSeeder implements ApplicationRunner {

    private final PermissionRepository permissionRepository;

    @Override
    public void run(ApplicationArguments args) {
        // The full catalog your app uses in @PreAuthorize / CustomAnnotationEvaluator
        List<Permission> catalog = List.of(
                p("ADMIN_ALL", "Tüm yönetici yetkileri"),
                p("VIEW_ROLES", "Rolleri görüntüleme"),
                p("EDIT_ROLES", "Rolleri düzenleme"),
                p("CREATE_ROLE", "Rol oluşturma"),
                p("VIEW_ALL_ATTENDANCE", "Herkesin devam durumunu görüntüleme"),
                p("EDIT_ALL_ATTENDANCE", "Herkesin devam durumunu düzenleme"),
                p("VIEW_CHILD_ATTENDANCE", "Alt departmanları görüntüleme"),
                p("VIEW_DEPARTMENT_ATTENDANCE", "Kendi departmanını görüntüleme"),
                p("EDIT_CHILD_ATTENDANCE", "Alt departmanları düzenleme"),
                p("EDIT_DEPARTMENT_ATTENDANCE", "Kendi departmanını düzenleme"),
                p("VIEW_ALL_USERS", "Tüm kullanıcıları görüntüleme"),
                p("VIEW_ALL_DEPARTMENTS", "Tüm departmanları görüntüleme"),
                p("VIEW_HOLIDAYS", "Tatilleri görüntüleme"),
                p("VIEW_DEPARTMENT_HIERARCHY", "Departman hiyerarşisini görüntüleme")
        );

        for (Permission target : catalog) {
            permissionRepository.findByName(target.getName())
                    .orElseGet(() -> {
                        log.info("Seeding permission {}", target.getName());
                        return permissionRepository.save(target);
                    });
        }
    }

    private static Permission p(String name, String desc) {
        Permission x = new Permission();
        x.setName(name);
        x.setDescription(desc);
        return x;
    }
}
