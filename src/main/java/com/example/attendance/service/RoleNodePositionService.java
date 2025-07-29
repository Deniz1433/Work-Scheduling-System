/**
 * RoleNodePositionService, rol pozisyonları ile ilgili işlemleri gerçekleştirir.
 */
// src/main/java/com/example/attendance/service/RoleNodePositionService.java
package com.example.attendance.service;

import com.example.attendance.dto.RoleNodePositionDto;
import com.example.attendance.model.RoleNodePosition;
import com.example.attendance.repository.RoleNodePositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoleNodePositionService {
    private final RoleNodePositionRepository repo;
    public RoleNodePositionService(RoleNodePositionRepository repo) {
        this.repo = repo;
    }

    /**
     * Tüm rol pozisyonlarını yükler.
     */
    public Map<String, RoleNodePosition> loadAll() {
        return repo.findAll()
                .stream()
                .collect(Collectors.toMap(RoleNodePosition::getRole, p -> p));
    }

    /**
     * Tüm rol pozisyonlarını kaydeder.
     */
    @Transactional
    public void saveAll(List<RoleNodePosition> positions) {
        // clear then insert
        repo.deleteAllInBatch();
        for (var pos : positions) {
            repo.save(pos);
        }
    }

    /**
     * Tüm rol pozisyonlarını DTO olarak döndürür.
     */
    public List<RoleNodePositionDto> getAllRoleNodePositionDtos() {
        return loadAll().values().stream()
            .map(e -> new RoleNodePositionDto(e.getRole(), e.getX(), e.getY()))
            .toList();
    }
}
