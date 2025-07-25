// src/main/java/com/example/attendance/controller/ExcuseController.java
package com.example.attendance.controller;

import com.example.attendance.dto.CreateExcuseDto;
import com.example.attendance.dto.ExcuseDto;
import com.example.attendance.service.ExcuseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/excuses")
@RequiredArgsConstructor
public class ExcuseController {

    private final ExcuseService excuseService;

    @GetMapping
    public List<ExcuseDto> list(@AuthenticationPrincipal OidcUser user) {
        return excuseService.listForUser(user.getSubject());
    }

    @PostMapping
    public ExcuseDto create(
            @Valid @RequestBody CreateExcuseDto dto,
            @AuthenticationPrincipal OidcUser user
    ) {
        return excuseService.create(user.getSubject(), dto);
    }

    @PutMapping("/{id}")
    public ExcuseDto update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateExcuseDto dto,
            @AuthenticationPrincipal OidcUser user
    ) {
        return excuseService.update(user.getSubject(), id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal OidcUser user
    ) {
        excuseService.delete(user.getSubject(), id);
    }
}
