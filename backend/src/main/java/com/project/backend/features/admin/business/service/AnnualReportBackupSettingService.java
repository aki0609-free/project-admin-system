package com.project.backend.features.admin.business.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.business.dto.AnnualReportBackupSettingResponse;
import com.project.backend.features.admin.business.dto.AnnualReportBackupSettingSaveRequest;
import com.project.backend.features.admin.business.entity.AnnualReportBackupSetting;
import com.project.backend.features.admin.business.repository.AnnualReportBackupSettingRepository;
import com.project.backend.features.operation.monthly.dto.AnnualReportBackupResult;
import com.project.backend.features.operation.monthly.service.AnnualReportBackupService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnnualReportBackupSettingService {

    private final AnnualReportBackupSettingRepository repository;
    private final AnnualReportBackupService backupService;

    @Transactional(readOnly = true)
    public AnnualReportBackupSettingResponse find() {
        return toResponse(findOrDefault(requireTenantId()));
    }

    @Transactional
    public AnnualReportBackupSettingResponse save(
            AnnualReportBackupSettingSaveRequest request
    ) {
        String tenantId = requireTenantId();
        AnnualReportBackupSetting setting = repository
                .findByTenantIdAndSettingCodeAndDeletedAtIsNull(
                        tenantId,
                        AnnualReportBackupSetting.DEFAULT_SETTING_CODE
                )
                .orElseGet(AnnualReportBackupSetting::new);
        setting.setSettingCode(
                AnnualReportBackupSetting.DEFAULT_SETTING_CODE
        );
        setting.setFiscalYearStartMonth(request.fiscalYearStartMonth());
        setting.setGraceDays(request.graceDays());
        setting.setStartupEnabled(request.startupEnabled());
        setting.setActiveFlag(request.activeFlag());
        return toResponse(repository.save(setting));
    }

    @Transactional
    public AnnualReportBackupResult execute(int fiscalYear) {
        AnnualReportBackupSetting setting = findOrDefault(
                requireTenantId()
        );
        if (!Boolean.TRUE.equals(setting.getActiveFlag())) {
            throw new IllegalStateException(
                    "年度帳票バックアップ設定が無効です。"
            );
        }
        return backupService.execute(
                fiscalYear,
                setting.getFiscalYearStartMonth(),
                setting.getGraceDays()
        );
    }

    private AnnualReportBackupSetting findOrDefault(String tenantId) {
        return repository
                .findByTenantIdAndSettingCodeAndDeletedAtIsNull(
                        tenantId,
                        AnnualReportBackupSetting.DEFAULT_SETTING_CODE
                )
                .orElseGet(AnnualReportBackupSetting::new);
    }

    private AnnualReportBackupSettingResponse toResponse(
            AnnualReportBackupSetting setting
    ) {
        return new AnnualReportBackupSettingResponse(
                setting.getFiscalYearStartMonth(),
                setting.getGraceDays(),
                setting.getStartupEnabled(),
                setting.getActiveFlag()
        );
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("テナント情報を取得できません。");
        }
        return tenantId;
    }
}
