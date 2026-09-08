package com.project.backend.features.employee.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.features.employee.entity.EmployeePayrollProfile;

import jakarta.persistence.LockModeType;

public interface EmployeePayrollProfileRepository extends JpaRepository<EmployeePayrollProfile, Long> {

    Optional<EmployeePayrollProfile> findByEmployeeIdAndDeletedAtIsNull(Long employeeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select profile
              from EmployeePayrollProfile profile
             where profile.employee.id = :employeeId
               and profile.deletedAt is null
            """)
    Optional<EmployeePayrollProfile> findForUpdateByEmployeeId(
            @Param("employeeId") Long employeeId
    );
}
