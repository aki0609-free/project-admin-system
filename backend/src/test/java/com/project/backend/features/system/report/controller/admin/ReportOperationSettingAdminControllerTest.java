package com.project.backend.features.system.report.controller.admin;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.backend.features.admin.business.dto.MonthlyClosingOutputAdminResponse;
import com.project.backend.features.admin.business.dto.PreviewReportSettingResponse;
import com.project.backend.features.admin.business.service.BusinessSettingService;
import com.project.backend.features.admin.business.service.PreviewReportSettingService;

@ExtendWith(MockitoExtension.class)
class ReportOperationSettingAdminControllerTest {

    @Mock
    private BusinessSettingService businessSettingService;

    @Mock
    private PreviewReportSettingService previewReportSettingService;

    private ReportOperationSettingAdminController controller;

    @BeforeEach
    void setUp() {
        controller = new ReportOperationSettingAdminController(
                businessSettingService,
                previewReportSettingService
        );
    }

    @Test
    void delegatesBothSettingListsToExistingServices() {
        List<MonthlyClosingOutputAdminResponse> closingOutputs = List.of();
        List<PreviewReportSettingResponse> previewReports = List.of();
        when(businessSettingService.findClosingOutputs()).thenReturn(closingOutputs);
        when(previewReportSettingService.findAll()).thenReturn(previewReports);

        assertSame(closingOutputs, controller.findClosingOutputs());
        assertSame(previewReports, controller.findPreviewReports());
    }
}
