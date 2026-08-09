package com.project.backend.features.operation.monthly.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.business.entity.AnnualReportBackupSetting;
import com.project.backend.features.admin.business.repository.AnnualReportBackupSettingRepository;
import com.project.backend.features.operation.monthly.enums.AnnualReportBackupStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnnualReportBackupStartupRunner {

    private static final Logger log = LoggerFactory.getLogger(
            AnnualReportBackupStartupRunner.class
    );

    private final AnnualReportBackupSettingRepository settingRepository;
    private final AnnualReportBackupService backupService;

    @EventListener(ApplicationReadyEvent.class)
    public void executeDueBackups() {
        for (AnnualReportBackupSetting setting : settingRepository
                .findByStartupEnabledTrueAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc()) {
            try {
                TenantContext.setTenantId(setting.getTenantId());
                for (Integer fiscalYear : backupService
                        .findPendingFiscalYears(
                                setting.getFiscalYearStartMonth(),
                                setting.getGraceDays()
                        )) {
                    var result = backupService.execute(
                            fiscalYear,
                            setting.getFiscalYearStartMonth(),
                            setting.getGraceDays()
                    );
                    if (result.status()
                            == AnnualReportBackupStatus.FAILED) {
                        log.error(
                                "Annual report backup failed. tenantId={}, fiscalYear={}, error={}",
                                setting.getTenantId(),
                                fiscalYear,
                                result.errorMessage()
                        );
                    } else {
                        log.info(
                                "Annual report backup completed. tenantId={}, fiscalYear={}, files={}",
                                setting.getTenantId(),
                                fiscalYear,
                                result.fileCount()
                        );
                    }
                }
            } catch (Exception exception) {
                log.error(
                        "Annual report backup startup catch-up failed. tenantId={}",
                        setting.getTenantId(),
                        exception
                );
            } finally {
                TenantContext.clear();
            }
        }
    }
}
