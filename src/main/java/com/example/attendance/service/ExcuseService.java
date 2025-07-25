// src/main/java/com/example/attendance/service/ExcuseService.java
package com.example.attendance.service;

import com.example.attendance.dto.CreateExcuseDto;
import com.example.attendance.dto.ExcuseDto;
import com.example.attendance.model.Excuse;
import com.example.attendance.model.User;
import com.example.attendance.repository.ExcuseRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcuseService {

    private final ExcuseRepository excuseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ExcuseDto> listForUser(String keycloakId) {
        return excuseRepository.findByUser_KeycloakId(keycloakId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExcuseDto create(String keycloakId, CreateExcuseDto dto) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Excuse exc = new Excuse();
        exc.setUser(user);
        exc.setStartDate(Date.from(dto.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        exc.setEndDate(Date.from(dto.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        exc.setReason(dto.getReason());
        exc.setDescription(dto.getDescription());

        Excuse saved = excuseRepository.save(exc);
        return toDto(saved);
    }

    @Transactional
    public ExcuseDto update(String keycloakId, UUID id, CreateExcuseDto dto) {
        Excuse exc = excuseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Excuse not found"));
        if (!exc.getUser().getKeycloakId().equals(keycloakId)) {
            throw new AccessDeniedException("Not your excuse");
        }

        exc.setStartDate(Date.from(dto.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        exc.setEndDate(Date.from(dto.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        exc.setReason(dto.getReason());
        exc.setDescription(dto.getDescription());
        exc.setStatus("pending");

        Excuse updated = excuseRepository.save(exc);
        return toDto(updated);
    }

    @Transactional
    public void delete(String keycloakId, UUID id) {
        Excuse exc = excuseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Excuse not found"));
        if (!exc.getUser().getKeycloakId().equals(keycloakId)) {
            throw new AccessDeniedException("Not your excuse");
        }
        excuseRepository.delete(exc);
    }

    private ExcuseDto toDto(Excuse exc) {
        ExcuseDto d = new ExcuseDto();
        d.setId(exc.getId());
        d.setStartDate(exc.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        d.setEndDate(exc.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        d.setReason(exc.getReason());
        d.setDescription(exc.getDescription());
        d.setStatus(exc.getStatus());
        d.setCreatedAt(exc.getCreatedAt()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime()
        );
        if (exc.getApprovedAt() != null) {
            d.setApprovedAt(exc.getApprovedAt()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime()
            );
        }
        return d;
    }
}
