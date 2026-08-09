package com.project.backend.features.admin.business.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.admin.business.entity.AnnualReportBackupSetting;

public interface AnnualReportBackupSettingRepository
        extends JpaRepository<AnnualReportBackupSetting, Long> {

    Optional<AnnualReportBackupSetting>
            findByTenantIdAndSettingCodeAndDeletedAtIsNull(
                    String tenantId,
                    String settingCode
            );

    List<AnnualReportBackupSetting>
            findByStartupEnabledTrueAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc();
}
