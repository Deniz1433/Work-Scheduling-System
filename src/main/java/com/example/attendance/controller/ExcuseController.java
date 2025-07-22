package com.example.attendance.controller;

import com.example.attendance.dto.ExcuseRequest;
import com.example.attendance.dto.ExcuseUpdateRequest;
import com.example.attendance.model.Excuse;
import com.example.attendance.service.ExcuseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/excuse")
public class ExcuseController {
    private final ExcuseService service;

    public ExcuseController(ExcuseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> submit(
            @RequestBody ExcuseRequest req,
            Principal principal
    ) {
        service.submitExcuse(principal.getName(), req);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<Excuse>> list(
            Principal principal
    ) {
        List<Excuse> excuses = service.listUserExcuses(principal.getName());
        return ResponseEntity.ok(excuses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            Principal principal
    ) {
        service.deleteExcuse(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody ExcuseUpdateRequest req,
            Principal principal
    ) {
        service.updateExcuse(principal.getName(), id, req);
        return ResponseEntity.ok().build();
    }
}
