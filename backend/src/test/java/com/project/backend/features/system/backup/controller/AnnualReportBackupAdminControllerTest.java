package com.project.backend.features.system.backup.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.backend.features.admin.business.dto.AnnualReportBackupSettingResponse;
import com.project.backend.features.admin.business.dto.AnnualReportBackupSettingSaveRequest;
import com.project.backend.features.admin.business.service.AnnualReportBackupSettingService;
import com.project.backend.features.operation.monthly.dto.AnnualReportBackupResult;
import com.project.backend.features.operation.monthly.enums.AnnualReportBackupStatus;

@ExtendWith(MockitoExtension.class)
class AnnualReportBackupAdminControllerTest {

    @Mock
    private AnnualReportBackupSettingService service;

    private AnnualReportBackupAdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AnnualReportBackupAdminController(service);
    }

    @Test
    void delegatesSettingsAndExecutionToExistingService() {
        AnnualReportBackupSettingResponse setting =
                new AnnualReportBackupSettingResponse(4, 14, true, true);
        AnnualReportBackupSettingSaveRequest request =
                new AnnualReportBackupSettingSaveRequest(4, 14, true, true);
        AnnualReportBackupResult result = new AnnualReportBackupResult(
                1L,
                2025,
                AnnualReportBackupStatus.COMPLETED,
                2,
                1024L,
                null
        );
        when(service.find()).thenReturn(setting);
        when(service.save(request)).thenReturn(setting);
        when(service.execute(2025)).thenReturn(result);

        assertSame(setting, controller.findSetting());
        assertSame(setting, controller.saveSetting(request));
        assertSame(result, controller.execute(2025));
        verify(service).find();
        verify(service).save(request);
        verify(service).execute(2025);
    }
}
