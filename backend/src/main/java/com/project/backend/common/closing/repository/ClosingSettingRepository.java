package com.project.backend.common.closing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.common.closing.entity.ClosingSetting;


public interface ClosingSettingRepository extends JpaRepository<ClosingSetting, Long> {

    Optional<ClosingSetting> findFirstBySettingCodeAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(
            String settingCode
    );
}