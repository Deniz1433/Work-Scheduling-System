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
                p("ADMIN_ALL", "All admin permissions"),
                p("VIEW_ROLES", "View roles"),
                p("EDIT_ROLES", "Edit roles"),
                p("CREATE_ROLE", "Create roles"),
                p("VIEW_ALL_ATTENDANCE", "View everyone’s attendance"),
                p("EDIT_ALL_ATTENDANCE", "Edit everyone’s attendance"),
                p("VIEW_CHILD_ATTENDANCE", "View child departments"),
                p("VIEW_DEPARTMENT_ATTENDANCE", "View own department"),
                p("EDIT_CHILD_ATTENDANCE", "Edit child departments"),
                p("EDIT_DEPARTMENT_ATTENDANCE", "Edit own department"),
                p("VIEW_ALL_USERS", "View all users"),
                p("VIEW_ALL_DEPARTMENTS", "View all departments"),
                p("VIEW_HOLIDAYS", "View holidays"),
                p("VIEW_DEPARTMENT_HIERARCHY", "View department hierarchy")
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
