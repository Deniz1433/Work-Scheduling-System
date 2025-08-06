// src/main/java/com/example/attendance/service/DepartmentHierarchyService.java
package com.example.attendance.service;


import com.example.attendance.model.Department;
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
    public void addRelation(Department parent, Department child) {
        if (parent.equals(child)) {
            throw new IllegalArgumentException("Cannot link department to itself");
        }
        repo.save(new DepartmentHierarchy(parent, child));
    }

    @Transactional
    public void removeRelation(Department parent, Department child) {
        repo.deleteByParentDepartmentAndChildDepartment(parent, child);
    }

    /**
     * Replace *all* relations with the provided list.
     * First clears the table in batch, then inserts the new ones.
     */
    @Transactional
    public void saveRelations(List<DepartmentHierarchy> relations) {
        // 1) clear existing rows in a single SQL statement
        repo.deleteAllInBatch();

        // 2) insert each new (parent, child) pair
        for (DepartmentHierarchy dh : relations) {
            if (!dh.getParentDepartment().equals(dh.getChildDepartment())) {
                repo.save(new DepartmentHierarchy(dh.getParentDepartment(), dh.getChildDepartment()));
            }
        }// bu fonksiyonu kontrol edin
    }

    /** Direct children of a role */
    public List<Department> findDirectChildren(Department parent) {
        return repo.findByParentDepartment(parent)
                .stream()
                .map(DepartmentHierarchy::getChildDepartment)
                .toList();
    }

    /** All descendants via DFS */
    public Set<Department> findAllDescendants(Department dep) {
        Set<Department> found = new LinkedHashSet<>();
        LinkedList<Department> stack = new LinkedList<>();
        stack.push(dep);
        while (!stack.isEmpty()) {
            Department cur = stack.pop();
            for (Department child : findDirectChildren(cur)) {
                if (found.add(child)) {
                    stack.push(child);
                }
            }
        }
        return found;
    }

    /** All ancestors (reverse graph) */
    public Set<Department> findAllAncestors(Department dep) {
        Set<Department> found = new LinkedHashSet<>();
        LinkedList<Department> stack = new LinkedList<>();
        stack.push(dep);
        while (!stack.isEmpty()) {
            Department cur = stack.pop();
            repo.findByChildDepartment(cur)
                    .stream()
                    .map(DepartmentHierarchy::getParentDepartment)
                    .forEach(parent -> {
                        if (found.add(parent)) stack.push(parent);
                    });
        }
        return found;
    }

    
}
