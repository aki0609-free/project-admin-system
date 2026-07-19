package com.project.backend.app.security.permission.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.app.security.permission.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Set<Role> findByNameIn(Set<String> names);

    @Query("""
        select distinct r
        from Role r
        left join fetch r.permissions
    """)
    List<Role> findAllWithPermissions();
    
    @Modifying
    @Query(value = "delete from role_permission where role_id = :roleId", nativeQuery = true)
    void deleteRolePermissionMappingsByRoleId(@Param("roleId") Long roleId);
}
