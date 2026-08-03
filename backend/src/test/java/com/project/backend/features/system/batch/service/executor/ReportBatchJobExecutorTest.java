package com.project.backend.features.system.batch.service.executor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.project.backend.features.system.batch.context.BatchJobExecutionContext;
import com.project.backend.features.system.batch.entity.BatchExecutionLog;
import com.project.backend.features.system.batch.enums.BatchExecutionTrigger;
import com.project.backend.features.system.report.service.api.ReportBatchExecutionService;

class ReportBatchJobExecutorTest {

    private final ReportBatchExecutionService reportService =
            Mockito.mock(ReportBatchExecutionService.class);
    private final ReportBatchJobExecutor executor =
            new ReportBatchJobExecutor(reportService);

    @Test
    void usesResolvedCustomerInvoiceReportCode() {
        Map<String, Object> params = Map.of(
                ReportBatchJobExecutor.RESOLVED_REPORT_CODE_PARAM,
                "MONTHLY_INVOICE_PATTERN_3"
        );
        BatchJobExecutionContext context = context(
                "MONTHLY_INVOICE",
                params
        );

        executor.execute(context);

        verify(reportService).execute(
                "MONTHLY_INVOICE_PATTERN_3",
                params
        );
    }

    @Test
    void rejectsDynamicResolutionForUnrelatedReportBatch() {
        BatchJobExecutionContext context = context(
                "MONTHLY_PAY_SLIP",
                Map.of(
                        ReportBatchJobExecutor.RESOLVED_REPORT_CODE_PARAM,
                        "MONTHLY_INVOICE_PATTERN_1"
                )
        );

        assertThatThrownBy(() -> executor.execute(context))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changesInvoiceExecutionModeToRetryForBatchRetry() {
        Map<String, Object> params = Map.of(
                ReportBatchJobExecutor.RESOLVED_REPORT_CODE_PARAM,
                "MONTHLY_INVOICE_PATTERN_2",
                "executionMode",
                "RECLOSE"
        );
        BatchJobExecutionContext context = context(
                "MONTHLY_INVOICE",
                params
        );
        BatchExecutionLog log = new BatchExecutionLog();
        log.setTriggerType(BatchExecutionTrigger.RETRY);
        when(context.log()).thenReturn(log);

        executor.execute(context);

        verify(reportService).execute(
                "MONTHLY_INVOICE_PATTERN_2",
                Map.of(
                        ReportBatchJobExecutor.RESOLVED_REPORT_CODE_PARAM,
                        "MONTHLY_INVOICE_PATTERN_2",
                        "executionMode",
                        "RETRY"
                )
        );
    }

    @Test
    void changesMonthlyExcelExecutionModeToRetryForBatchRetry() {
        Map<String, Object> params = Map.of(
                "targetMonth", "2026-07",
                "closingVersion", 1,
                "executionMode", "INITIAL"
        );
        BatchJobExecutionContext context = context(
                "MONTHLY_LABOR_COST_LIST",
                params
        );
        BatchExecutionLog log = new BatchExecutionLog();
        log.setTriggerType(BatchExecutionTrigger.RETRY);
        when(context.log()).thenReturn(log);

        executor.execute(context);

        verify(reportService).execute(
                "MONTHLY_LABOR_COST_LIST",
                Map.of(
                        "targetMonth", "2026-07",
                        "closingVersion", 1,
                        "executionMode", "RETRY"
                )
        );
    }

    private BatchJobExecutionContext context(
            String targetCode,
            Map<String, Object> params
    ) {
        BatchJobExecutionContext context =
                Mockito.mock(BatchJobExecutionContext.class);
        when(context.targetCode()).thenReturn(targetCode);
        when(context.params()).thenReturn(params);
        return context;
    }
}
