package com.project.backend.features.employee.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.employee.entity.EmployeePayrollProfile;

public interface EmployeePayrollProfileRepository extends JpaRepository<EmployeePayrollProfile, Long> {

    Optional<EmployeePayrollProfile> findByEmployeeIdAndDeletedAtIsNull(Long employeeId);
}