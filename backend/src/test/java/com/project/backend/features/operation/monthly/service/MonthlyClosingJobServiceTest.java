package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.project.backend.features.operation.book.service.SpreadsheetLedgerGenerationService;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.service.executor.MonthlyClosingJobExecutor;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewRepository;

class MonthlyClosingJobServiceTest {

    private OperationReportPreviewRepository previewRepository;
    private MonthlyClosingJobExecutor executor;
    private SpreadsheetLedgerGenerationService ledgerService;
    private MonthlyClosingOutputDefinitionService definitionService;
    private MonthlyClosingJobService service;

    @BeforeEach
    void setUp() {
        previewRepository = mock(OperationReportPreviewRepository.class);
        executor = mock(MonthlyClosingJobExecutor.class);
        ledgerService = mock(SpreadsheetLedgerGenerationService.class);
        definitionService = mock(
                MonthlyClosingOutputDefinitionService.class
        );
        service = new MonthlyClosingJobService(
                previewRepository,
                executor,
                ledgerService,
                definitionService
        );
    }

    @Test
    void executeClosing_shouldFollowMasterExecutionOrder() {
        MonthlyClosingOutputDefinition report = definition(
                MonthlyClosingOutputType.REPORT,
                "MONTHLY_PAY_SLIP"
        );
        MonthlyClosingOutputDefinition ledger = definition(
                MonthlyClosingOutputType.LEDGER,
                "MONTHLY_LABOR"
        );
        OperationReportPreview preview = preview(
                "MONTHLY_PAY_SLIP",
                "PRINT_MONTHLY_PAY_SLIP"
        );
        when(previewRepository
                .findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                        OperationType.MONTHLY,
                        "MONTHLY_PAY_SLIP"
                )).thenReturn(Optional.of(preview));

        service.executeClosing(
                10L,
                period(),
                3,
                List.of(report, ledger)
        );

        InOrder order = inOrder(executor, ledgerService);
        order.verify(executor).execute(10L, preview, period(), 3);
        order.verify(ledgerService).generateForClosing(
                "MONTHLY_LABOR",
                "2026-07",
                3
        );
    }

    @Test
    void executeClosing_shouldUseCompanyDefinitionsForDefaultCall() {
        MonthlyClosingOutputDefinition ledger = definition(
                MonthlyClosingOutputType.LEDGER,
                "MONTHLY_CASH_RECEIPTS"
        );
        when(definitionService.findActiveCompanyOutputs())
                .thenReturn(List.of(ledger));

        service.executeClosing(10L, period(), 1);

        verify(ledgerService).generateForClosing(
                "MONTHLY_CASH_RECEIPTS",
                "2026-07",
                1
        );
    }

    @Test
    void executeClosing_shouldRejectMissingExecutionPlan() {
        assertThatThrownBy(() -> service.executeClosing(
                10L,
                period(),
                1,
                List.of()
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("設定");
        verify(executor, never()).execute(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void executeClosing_shouldRejectInactiveReportPreview() {
        MonthlyClosingOutputDefinition report = definition(
                MonthlyClosingOutputType.REPORT,
                "MONTHLY_PAY_SLIP"
        );
        OperationReportPreview preview = preview(
                "MONTHLY_PAY_SLIP",
                "PRINT_MONTHLY_PAY_SLIP"
        );
        preview.setActiveFlag(false);
        when(previewRepository
                .findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                        OperationType.MONTHLY,
                        "MONTHLY_PAY_SLIP"
                )).thenReturn(Optional.of(preview));

        assertThatThrownBy(() -> service.executeClosing(
                10L,
                period(),
                1,
                List.of(report)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("有効な締め帳票");
    }

    private MonthlyClosingOutputDefinition definition(
            MonthlyClosingOutputType type,
            String outputCode
    ) {
        MonthlyClosingOutputDefinition definition =
                new MonthlyClosingOutputDefinition();
        definition.setOutputType(type);
        definition.setOutputCode(outputCode);
        definition.setRequiredFlag(true);
        definition.setActiveFlag(true);
        return definition;
    }

    private OperationReportPreview preview(
            String reportCode,
            String jobCode
    ) {
        OperationReportPreview preview = new OperationReportPreview();
        preview.setReportCode(reportCode);
        preview.setJobCode(jobCode);
        preview.setActiveFlag(true);
        return preview;
    }

    private MonthlyClosingPeriod period() {
        return new MonthlyClosingPeriod(
                "2026-07",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                null
        );
    }
}
