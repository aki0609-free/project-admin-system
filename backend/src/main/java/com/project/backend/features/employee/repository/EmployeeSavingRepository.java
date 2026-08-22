package com.project.backend.features.employee.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.employee.entity.EmployeeSaving;

public interface EmployeeSavingRepository extends JpaRepository<EmployeeSaving, Long> {

    List<EmployeeSaving> findAllByDeletedAtIsNullOrderByIdDesc();

    Optional<EmployeeSaving> findByIdAndDeletedAtIsNull(Long id);

    Optional<EmployeeSaving> findFirstByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(
            Long employeeId
    );

    boolean existsByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNull(Long employeeId);

    boolean existsByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNullAndIdNot(
            Long employeeId,
            Long id
    );

    boolean existsByEmployeeIdAndDeletedAtIsNull(Long employeeId);
}
