// src/main/java/com/example/attendance/service/RoleHierarchyService.java
package com.example.attendance.service;

import com.example.attendance.model.RoleHierarchy;
import com.example.attendance.repository.RoleHierarchyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleHierarchyService {
    private final RoleHierarchyRepository repo;

    public RoleHierarchyService(RoleHierarchyRepository repo) {
        this.repo = repo;
    }

    public List<RoleHierarchy> listAll() {
        return repo.findAll();
    }

    @Transactional
    public void addRelation(String parent, String child) {
        if (parent.equals(child)) {
            throw new IllegalArgumentException("Cannot link role to itself");
        }
        repo.save(new RoleHierarchy(parent, child));
    }

    @Transactional
    public void removeRelation(String parent, String child) {
        repo.deleteByParentRoleAndChildRole(parent, child);
    }

    /** Direct children of a role */
    public List<String> findDirectChildren(String parent) {
        return repo.findByParentRole(parent)
                .stream()
                .map(RoleHierarchy::getChildRole)
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
                    .map(RoleHierarchy::getParentRole)
                    .forEach(parent -> {
                        if (found.add(parent)) stack.push(parent);
                    });
        }
        return found;
    }
}
