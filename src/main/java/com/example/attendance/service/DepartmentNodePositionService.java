// src/main/java/com/example/attendance/service/RoleNodePositionService.java
package com.example.attendance.service;

import com.example.attendance.model.DepartmentNodePosition;
import com.example.attendance.repository.DepartmentNodePositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DepartmentNodePositionService {
    private final DepartmentNodePositionRepository repo;
    public DepartmentNodePositionService(DepartmentNodePositionRepository repo) {
        this.repo = repo;
    }

    public Map<String, DepartmentNodePosition> loadAll() {
        return repo.findAll()
                .stream()
                .collect(Collectors.toMap(DepartmentNodePosition::getRole, p -> p));
    }

    @Transactional
    public void saveAll(List<DepartmentNodePosition> positions) {
        // clear then insert
        repo.deleteAllInBatch();
        for (var pos : positions) {
            repo.save(pos);
        }
    }
}
