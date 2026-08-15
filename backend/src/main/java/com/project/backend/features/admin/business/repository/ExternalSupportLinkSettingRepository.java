package com.project.backend.features.admin.business.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.admin.business.entity.ExternalSupportLinkSetting;

public interface ExternalSupportLinkSettingRepository
        extends JpaRepository<ExternalSupportLinkSetting, Long> {

    Optional<ExternalSupportLinkSetting>
            findByTenantIdAndSettingCodeAndDeletedAtIsNull(
                    String tenantId,
                    String settingCode
            );
}
