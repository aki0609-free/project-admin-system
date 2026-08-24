package com.project.backend.features.admin.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.common.closing.repository.ClosingSettingRepository;
import com.project.backend.features.admin.business.dto.MonthlyClosingOutputSaveRequest;
import com.project.backend.features.employee.repository.EmployeeResignationChecklistRepository;
import com.project.backend.features.employee.repository.EmployeeResignationSettingRepository;
import com.project.backend.features.employee.entity.EmployeeResignationChecklistMaster;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingOutputDefinitionRepository;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewRepository;

class BusinessSettingServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-22T00:00:00Z"),
            ZoneOffset.UTC
    );

    private MonthlyClosingOutputDefinitionRepository definitionRepository;
    private OperationReportPreviewRepository previewRepository;
    private EmployeeResignationChecklistRepository checklistRepository;
    private BusinessSettingService service;

    @BeforeEach
    void setUp() {
        definitionRepository = mock(MonthlyClosingOutputDefinitionRepository.class);
        previewRepository = mock(OperationReportPreviewRepository.class);
        checklistRepository = mock(EmployeeResignationChecklistRepository.class);
        service = new BusinessSettingService(
                mock(EmployeeResignationSettingRepository.class),
                checklistRepository,
                mock(ClosingSettingRepository.class),
                definitionRepository,
                previewRepository,
                CLOCK
        );
    }

    @Test
    void deleteChecklist_shouldUseBusinessClock() {
        EmployeeResignationChecklistMaster checklist =
                new EmployeeResignationChecklistMaster();
        when(checklistRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(checklist));

        service.deleteChecklist(10L);

        assertThat(checklist.getDeletedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    void findClosingOutputs_shouldMergeReportCatalogAndClosingSetting() {
        OperationReportPreview preview = preview(true);
        MonthlyClosingOutputDefinition definition =
                new MonthlyClosingOutputDefinition();
        definition.setId(10L);
        definition.setOutputType(MonthlyClosingOutputType.REPORT);
        definition.setOutputCode("MONTHLY_PAY_SLIP");
        definition.setExecutionOrder(20);
        definition.setRequiredFlag(true);
        definition.setActiveFlag(false);
        definition.setBackupRetentionYears(7);
        when(definitionRepository
                .findByOutputTypeAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc(
                        MonthlyClosingOutputType.REPORT
                )).thenReturn(List.of(definition));
        when(previewRepository
                .findByOperationTypeAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        OperationType.MONTHLY
                )).thenReturn(List.of(preview));

        var result = service.findClosingOutputs();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.reportCode()).isEqualTo("MONTHLY_PAY_SLIP");
            assertThat(item.executionOrder()).isEqualTo(20);
            assertThat(item.activeFlag()).isFalse();
            assertThat(item.backupRetentionYears()).isEqualTo(7);
        });
    }

    @Test
    void saveClosingOutputs_shouldPersistSelectionForActiveReport() {
        OperationReportPreview preview = preview(true);
        when(previewRepository.findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                OperationType.MONTHLY,
                "MONTHLY_PAY_SLIP"
        )).thenReturn(Optional.of(preview));
        when(definitionRepository.findByOutputTypeAndOutputCodeAndDeletedAtIsNull(
                MonthlyClosingOutputType.REPORT,
                "MONTHLY_PAY_SLIP"
        )).thenReturn(Optional.empty());
        when(previewRepository
                .findByOperationTypeAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        OperationType.MONTHLY
                )).thenReturn(List.of(preview));
        when(definitionRepository
                .findByOutputTypeAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc(
                        MonthlyClosingOutputType.REPORT
                )).thenReturn(List.of());

        service.saveClosingOutputs(List.of(
                new MonthlyClosingOutputSaveRequest(
                        "MONTHLY_PAY_SLIP",
                        10,
                        true,
                        7
                )
        ));

        verify(definitionRepository).save(
                org.mockito.ArgumentMatchers.argThat(definition ->
                        definition.getOutputCode().equals("MONTHLY_PAY_SLIP")
                                && definition.getExecutionOrder() == 10
                                && Boolean.TRUE.equals(definition.getRequiredFlag())
                )
        );
    }

    @Test
    void saveClosingOutputs_shouldRejectReportDisabledInReportCatalog() {
        OperationReportPreview preview = preview(false);
        when(previewRepository.findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                OperationType.MONTHLY,
                "MONTHLY_PAY_SLIP"
        )).thenReturn(Optional.of(preview));

        assertThatThrownBy(() -> service.saveClosingOutputs(List.of(
                new MonthlyClosingOutputSaveRequest(
                        "MONTHLY_PAY_SLIP",
                        10,
                        true,
                        7
                )
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("帳票管理で無効");
    }

    private OperationReportPreview preview(boolean active) {
        OperationReportPreview preview = new OperationReportPreview();
        preview.setReportCode("MONTHLY_PAY_SLIP");
        preview.setReportName("月次給与明細");
        preview.setJobCode("PRINT_MONTHLY_PAY_SLIP");
        preview.setOutputType(OperationReportOutputType.PDF);
        preview.setDisplayOrder(10);
        preview.setActiveFlag(active);
        return preview;
    }
}
