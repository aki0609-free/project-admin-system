package com.project.backend.features.employee.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.employee.entity.EmployeeContract;

public interface EmployeeContractRepository extends JpaRepository<EmployeeContract, Long> {

    Optional<EmployeeContract> findByEmployeeIdAndDeletedAtIsNull(Long employeeId);
}