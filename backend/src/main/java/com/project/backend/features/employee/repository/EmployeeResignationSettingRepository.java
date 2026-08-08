package com.project.backend.features.employee.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.employee.entity.EmployeeResignationSetting;

public interface EmployeeResignationSettingRepository
        extends JpaRepository<EmployeeResignationSetting, Long> {

    Optional<EmployeeResignationSetting> findBySettingCodeAndDeletedAtIsNull(
            String settingCode
    );
}
