package com.project.backend.features.employee.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.employee.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findAllByDeletedAtIsNullOrderByIdAsc();

    Optional<Employee> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByEmployeeCodeAndDeletedAtIsNull(String employeeCode);

    boolean existsByEmployeeCodeAndIdNotAndDeletedAtIsNull(String employeeCode, Long id);
}