package com.example.attendance.service;

import com.example.attendance.dto.ExcusesRequest;
import com.example.attendance.dto.ExcuseUpdateRequest;
import com.example.attendance.model.Excuse;
import com.example.attendance.repository.ExcuseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcuseService {
    private final ExcuseRepository repo;

    public ExcuseService(ExcuseRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void submitExcuses(Long userId, ExcusesRequest req) {
        List<Excuse> excuses = req.getDates().stream() /* 
        excuse database'inin her hafta temizleneceği varsayıldı, 
        belli aralıktakileri döndürecek şekilde güncellenebilir
        */ 
                .map(d -> new Excuse(userId, LocalDate.parse(d), req.getExcuseType(), req.getDescription()))
                .collect(Collectors.toList());
        repo.saveAll(excuses);
    }

    public List<Excuse> listUserExcuses(Long userId) {
        return repo.findByUserId(userId);
    }

    @Transactional
    public void deleteExcuse(Long userId, Long excuseId) {
        Excuse e = repo.findById(excuseId).orElseThrow();
        if(e != null){
            repo.delete(e);
        }
    }

    @Transactional
    public void updateExcuse(Long userId, Long id ,ExcuseUpdateRequest req) {
        Excuse e = repo.findById(id).orElseThrow();
        if(e != null){
            if (!e.getUserId().equals(userId)) {
                throw new AccessDeniedException("Not your excuse");
            }
            else{
                e.setExcuseType(req.getExcuseType());
                e.setDescription(req.getDescription());
                e.setIsApproved(false);
                repo.save(e);
            }
        }
    }
}
