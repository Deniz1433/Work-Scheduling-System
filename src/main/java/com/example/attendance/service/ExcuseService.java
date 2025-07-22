package com.example.attendance.service;

import com.example.attendance.dto.ExcuseRequest;
import com.example.attendance.dto.ExcuseUpdateRequest;
import com.example.attendance.model.Excuse;
import com.example.attendance.repository.ExcuseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcuseService {
    private final ExcuseRepository repo;

    public ExcuseService(ExcuseRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void submitExcuse(String userId, ExcuseRequest req) {
        List<Excuse> excuses = req.getDates().stream()
                .map(d -> new Excuse(userId, d, req.getExcuseType(), req.getDescription()))
                .collect(Collectors.toList());
        repo.saveAll(excuses);
    }

    public List<Excuse> listUserExcuses(String userId) {
        return repo.findByUserId(userId);
    }

    @Transactional
    public void deleteExcuse(String userId, Long id) {
        Excuse e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Excuse not found"));
        if (!e.getUserId().equals(userId)) {
            throw new AccessDeniedException("Not your excuse");
        }
        repo.delete(e);
    }

    @Transactional
    public void updateExcuse(String userId, Long id, ExcuseUpdateRequest req) {
        Excuse e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Excuse not found"));
        if (!e.getUserId().equals(userId)) {
            throw new AccessDeniedException("Not your excuse");
        }
        e.setExcuseType(req.getExcuseType());
        e.setDescription(req.getDescription());
        e.setIsApproved(false);
        repo.save(e);
    }
}
