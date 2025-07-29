// src/main/java/com/example/attendance/service/RoleHierarchyService.java
package com.example.attendance.service;

import com.example.attendance.model.RoleHierarchy;
import com.example.attendance.repository.RoleHierarchyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayDeque;
import org.keycloak.representations.idm.RoleRepresentation;
import com.example.attendance.dto.RoleHierarchyDto;
import com.example.attendance.dto.RoleNodePositionDto;
import java.util.stream.Collectors;

/**
 * RoleHierarchyService, rol hiyerarşisi ile ilgili işlemleri gerçekleştirir.
 */
@Service
public class RoleHierarchyService {
    private final RoleHierarchyRepository repo;

    public RoleHierarchyService(RoleHierarchyRepository repo) {
        this.repo = repo;
    }

    /**
     * Tüm rol ilişkilerini listeler.
     */
    public List<RoleHierarchy> listAll() {
        return repo.findAll();
    }

    /**
     * Tüm rol ilişkilerini DTO olarak döndürür.
     */
    public List<RoleHierarchyDto> getAllRoleHierarchyDtos() {
        return listAll().stream()
            .map(e -> new RoleHierarchyDto(e.getParentRole(), e.getChildRole()))
            .toList();
    }

    /**
     * Yeni bir rol ilişkisi ekler.
     */
    @Transactional
    public void addRelation(String parent, String child) {
        if (parent == null || parent.isBlank() || child == null || child.isBlank()) {
            throw new IllegalArgumentException("Parent and child roles must not be null or blank");
        }
        if (parent.equals(child)) {
            throw new IllegalArgumentException("Cannot link role to itself");
        }
        repo.save(new RoleHierarchy(parent, child));
    }

    /**
     * Belirtilen rol ilişkisini siler.
     */
    @Transactional
    public void removeRelation(String parent, String child) {
        if (parent == null || parent.isBlank() || child == null || child.isBlank()) {
            throw new IllegalArgumentException("Parent and child roles must not be null or blank");
        }
        repo.deleteByParentRoleAndChildRole(parent, child);
    }

    /**
     * Tüm ilişkileri verilen listeyle değiştirir.
     */
    @Transactional
    public void saveRelations(List<RoleRelationDto> relations) {
        // 1) clear existing rows in a single SQL statement
        repo.deleteAllInBatch();

        // 2) insert each new (parent, child) pair
        for (RoleRelationDto dto : relations) {
            if (!dto.getParent().equals(dto.getChild())) {
                repo.save(new RoleHierarchy(dto.getParent(), dto.getChild()));
            }
        }
    }

    /**
     * Bir rolün doğrudan çocuk rollerini döndürür.
     */
    public List<String> findDirectChildren(String parent) {
        return repo.findByParentRole(parent)
                .stream()
                .map(RoleHierarchy::getChildRole)
                .toList();
    }

    /**
     * Bir rolün tüm alt rollerini (descendants) döndürür.
     */
    public Set<String> findAllDescendants(String role) {
        Set<String> found = new LinkedHashSet<>();
        ArrayDeque<String> stack = new ArrayDeque<>();
        stack.push(role);
        // Tüm ilişkileri başta belleğe al
        List<RoleHierarchy> allLinks = repo.findAll();
        Map<String, List<String>> childrenMap = buildChildrenMap(allLinks);
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            for (String child : childrenMap.getOrDefault(cur, List.of())) {
                if (found.add(child)) {
                    stack.push(child);
                }
            }
        }
        return found;
    }

    /**
     * Bir rolün tüm üst rollerini (ancestors) döndürür.
     */
    public Set<String> findAllAncestors(String role) {
        Set<String> found = new LinkedHashSet<>();
        ArrayDeque<String> stack = new ArrayDeque<>();
        stack.push(role);
        // Tüm ilişkileri başta belleğe al
        List<RoleHierarchy> allLinks = repo.findAll();
        Map<String, List<String>> parentMap = new HashMap<>();
        for (var l : allLinks) {
            parentMap.computeIfAbsent(l.getChildRole(), k -> new ArrayList<>())
                     .add(l.getParentRole());
        }
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            for (String parent : parentMap.getOrDefault(cur, List.of())) {
                if (found.add(parent)) {
                    stack.push(parent);
                }
            }
        }
        return found;
    }

    /**
     * Filtrelenmiş rolleri (base roller hariç) döndürür.
     */
    public List<String> getFilteredRoles(List<RoleRepresentation> allRoles, Set<String> baseRoles) {
        return allRoles.stream()
                .map(RoleRepresentation::getName)
                .filter(r -> !baseRoles.contains(r))
                .toList();
    }

    /**
     * Tüm ilişkilerden parent-child mapini oluşturur.
     */
    public Map<String, List<String>> buildChildrenMap(List<RoleHierarchy> links) {
        Map<String, List<String>> childrenMap = new HashMap<>();
        for (var l : links) {
            childrenMap.computeIfAbsent(l.getParentRole(), k -> new ArrayList<>())
                    .add(l.getChildRole());
        }
        return childrenMap;
    }

    /**
     * Kök rollerin listesini döndürür.
     */
    public List<String> getRootRoles(List<String> roles, List<RoleHierarchy> links) {
        Set<String> allChildren = links.stream()
                .map(RoleHierarchy::getChildRole)
                .collect(Collectors.toSet());
        return roles.stream()
                .filter(r -> !allChildren.contains(r))
                .toList();
    }

    /**
     * (DTO) Bir rol ilişkisini temsil eder.
     */
    public static class RoleRelationDto {
        private String parent;
        private String child;
        public String getParent() { return parent; }
        public void setParent(String parent) { this.parent = parent; }
        public String getChild() { return child; }
        public void setChild(String child) { this.child = child; }
    }
}
