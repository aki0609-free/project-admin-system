package com.project.backend.features.employee.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.employee.entity.EmployeeResignationChecklistMaster;

public interface EmployeeResignationChecklistRepository
        extends JpaRepository<EmployeeResignationChecklistMaster, Long> {

    List<EmployeeResignationChecklistMaster>
            findAllByActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc();

    List<EmployeeResignationChecklistMaster>
            findAllByActiveFlagTrueAndRequiredFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc();
}