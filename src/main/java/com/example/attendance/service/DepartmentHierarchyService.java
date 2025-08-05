// src/main/java/com/example/attendance/service/RoleHierarchyService.java
package com.example.attendance.service;

import com.example.attendance.model.DepartmentHierarchy;
import com.example.attendance.repository.DepartmentHierarchyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Service
public class DepartmentHierarchyService {
    private final DepartmentHierarchyRepository repo;

    public DepartmentHierarchyService(DepartmentHierarchyRepository repo) {
        this.repo = repo;
    }

    public List<DepartmentHierarchy> listAll() {
        return repo.findAll();
    }

    @Transactional
    public void addRelation(String parent, String child) {
        if (parent.equals(child)) {
            throw new IllegalArgumentException("Cannot link role to itself");
        }
        repo.save(new DepartmentHierarchy(parent, child));
    }

    @Transactional
    public void removeRelation(String parent, String child) {
        repo.deleteByParentRoleAndChildRole(parent, child);
    }

    /**
     * Replace *all* relations with the provided list.
     * First clears the table in batch, then inserts the new ones.
     */
    @Transactional
    public void saveRelations(List<RoleRelationDto> relations) {
        // 1) clear existing rows in a single SQL statement
        repo.deleteAllInBatch();

        // 2) insert each new (parent, child) pair
        for (RoleRelationDto dto : relations) {
            if (!dto.getParent().equals(dto.getChild())) {
                repo.save(new DepartmentHierarchy(dto.getParent(), dto.getChild()));
            }
        }
    }

    /** Direct children of a role */
    public List<String> findDirectChildren(String parent) {
        return repo.findByParentRole(parent)
                .stream()
                .map(DepartmentHierarchy::getChildRole)
                .toList();
    }

    /** All descendants via DFS */
    public Set<String> findAllDescendants(String role) {
        Set<String> found = new LinkedHashSet<>();
        LinkedList<String> stack = new LinkedList<>();
        stack.push(role);
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            for (String child : findDirectChildren(cur)) {
                if (found.add(child)) {
                    stack.push(child);
                }
            }
        }
        return found;
    }

    /** All ancestors (reverse graph) */
    public Set<String> findAllAncestors(String role) {
        Set<String> found = new LinkedHashSet<>();
        LinkedList<String> stack = new LinkedList<>();
        stack.push(role);
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            repo.findByChildRole(cur)
                    .stream()
                    .map(DepartmentHierarchy::getParentRole)
                    .forEach(parent -> {
                        if (found.add(parent)) stack.push(parent);
                    });
        }
        return found;
    }

    /**
     * DTO used by the save endpoint to represent a (parent, child) link.
     */
    public static class RoleRelationDto {
        private String parent;
        private String child;

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }

        public String getChild() {
            return child;
        }

        public void setChild(String child) {
            this.child = child;
        }
    }
}
