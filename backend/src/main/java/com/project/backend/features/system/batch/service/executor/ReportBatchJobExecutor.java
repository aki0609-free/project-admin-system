package com.project.backend.features.system.batch.service.executor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.batch.context.BatchJobExecutionContext;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.enums.BatchExecutionTrigger;
import com.project.backend.features.system.batch.enums.BatchJobType;
import com.project.backend.features.system.report.service.api.ReportBatchExecutionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportBatchJobExecutor implements BatchJobExecutor {

    public static final String RESOLVED_REPORT_CODE_PARAM =
            "resolvedReportCode";

    private final ReportBatchExecutionService reportBatchExecutionService;

    @Override
    public BatchJobType supportType() {
        return BatchJobType.REPORT;
    }

    @Override
    public BatchJobExecutionResult execute(BatchJobExecutionContext context) {
        String reportCode = resolveReportCode(context);
        Map<String, Object> params = resolveExecutionParams(
                context,
                reportCode
        );

        return reportBatchExecutionService.execute(
            reportCode,
            params
        );
    }

    private Map<String, Object> resolveExecutionParams(
            BatchJobExecutionContext context,
            String reportCode
    ) {
        Map<String, Object> params = context.params();
        if (context.log() == null
                || context.log().getTriggerType()
                        != BatchExecutionTrigger.RETRY
                || params == null
                || !params.containsKey("executionMode")) {
            return params;
        }

        Map<String, Object> retryParams = new HashMap<>(params);
        retryParams.put("executionMode", "RETRY");
        return Map.copyOf(retryParams);
    }

    private String resolveReportCode(BatchJobExecutionContext context) {
        Map<String, Object> params = context.params();
        Object candidate = params != null
                ? params.get(RESOLVED_REPORT_CODE_PARAM)
                : null;

        if (!(candidate instanceof String resolved)
                || !resolved.startsWith("MONTHLY_INVOICE_PATTERN_")) {
            return context.targetCode();
        }

        String targetCode = context.targetCode();
        if (!"MONTHLY_INVOICE".equals(targetCode)
                && !targetCode.startsWith("MONTHLY_INVOICE_PATTERN_")) {
            throw new IllegalArgumentException(
                    "帳票コードの動的解決を許可されていないバッチです。 targetCode="
                            + targetCode
            );
        }

        return resolved;
    }
}
