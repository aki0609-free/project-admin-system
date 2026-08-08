package com.project.backend.features.employee.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.employee.entity.EmployeeResignationChecklistMaster;

public interface EmployeeResignationChecklistRepository
        extends JpaRepository<EmployeeResignationChecklistMaster, Long> {

    List<EmployeeResignationChecklistMaster>
            findAllByActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc();

    List<EmployeeResignationChecklistMaster>
            findAllByActiveFlagTrueAndRequiredFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc();

    List<EmployeeResignationChecklistMaster>
            findAllByDeletedAtIsNullOrderByDisplayOrderAscIdAsc();

    Optional<EmployeeResignationChecklistMaster> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndIdNotAndDeletedAtIsNull(String code, Long id);
}
