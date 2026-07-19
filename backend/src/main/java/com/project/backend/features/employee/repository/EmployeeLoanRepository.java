package com.project.backend.features.employee.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.employee.entity.EmployeeLoan;

public interface EmployeeLoanRepository extends JpaRepository<EmployeeLoan, Long> {

    List<EmployeeLoan> findAllByDeletedAtIsNullOrderByIdDesc();

    Optional<EmployeeLoan> findByIdAndDeletedAtIsNull(Long id);

    Optional<EmployeeLoan> findFirstByEmployeeIdAndActiveFlagTrueOrderByIdDesc(
        Long employeeId
    );
}