package com.project.backend.features.employee.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.employee.entity.EmployeeTimesheet;

public interface EmployeeTimesheetRepository extends JpaRepository<EmployeeTimesheet, Long> {

    List<EmployeeTimesheet> findAllByDeletedAtIsNullOrderByWorkDateDescIdDesc();

    Optional<EmployeeTimesheet> findByIdAndDeletedAtIsNull(Long id);
}