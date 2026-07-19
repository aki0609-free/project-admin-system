package com.project.backend.app.security.permission.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.app.security.permission.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    Optional<Permission> findByName(String name);

    Set<Permission> findByIdIn(Set<Long> ids);
}
