package com.project.backend.features.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.features.user.entity.User;


public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsernameAndTenantId(
        String username,
        String tenantId
    );

    Optional<User> findByUsername(String username);

    @Query("""
    SELECT u FROM User u WHERE u.deletedAt IS NOT NULL        
    """)
    List<User> findDeleted();

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"roles"})
    List<User> findAllByOrderByIdAsc();

    @Query("""
        select distinct u
        from User u
        left join fetch u.roles r
        left join fetch r.permissions
        where u.id = :id
    """)
    Optional<User> findDetailById(Long id);

    @Query("""
        select case when count(u) > 0 then true else false end
        from User u
        join u.roles r
        where r.id = :roleId
          and u.deletedAt is null
    """)
    boolean existsActiveUserByRoleId(Long roleId);

    @Modifying
    @Query(value = "delete from user_role where role_id = :roleId", nativeQuery = true)
    void deleteUserRoleMappingsByRoleId(@Param("roleId") Long roleId);
}
