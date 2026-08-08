package com.project.backend.features.admin.business.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.admin.business.entity.DormitoryFeeSetting;
import com.project.backend.features.employee.enums.DormitoryType;

public interface DormitoryFeeSettingRepository
        extends JpaRepository<DormitoryFeeSetting, Long> {

    List<DormitoryFeeSetting> findAllByDeletedAtIsNullOrderByDormitoryTypeAsc();

    Optional<DormitoryFeeSetting> findByDormitoryTypeAndDeletedAtIsNull(
            DormitoryType dormitoryType
    );

    Optional<DormitoryFeeSetting> findByDormitoryTypeAndActiveFlagTrueAndDeletedAtIsNull(
            DormitoryType dormitoryType
    );
}
