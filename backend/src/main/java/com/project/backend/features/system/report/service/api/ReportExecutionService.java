package com.project.backend.features.system.report.service.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.sql.service.DynamicTableInsertService;
import com.project.backend.features.system.report.dto.ReportExecuteResponse;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportParam;
import com.project.backend.features.system.report.service.api.preprocess.ReportPreProcessExecutorResolver;
import com.project.backend.features.system.report.service.builder.ReportInputBindBuilder;
import com.project.backend.features.system.report.service.core.ReportDefinitionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportExecutionService {

    private final ReportDefinitionService reportDefinitionService;
    private final ReportInputBindBuilder reportInputBindBuilder;
    private final DynamicTableInsertService dynamicTableInsertService;
    private final ReportHistoryService reportHistoryService;
    private final ReportPreProcessExecutorResolver preProcessExecutorResolver;

    @Transactional
    public ReportExecuteResponse prepareExecution(
            String reportCode,
            Map<String, Object> requestParams
    ) {
        ReportMaster reportMaster =
                reportDefinitionService.getActiveReport(reportCode);

        List<ReportParam> paramDefinitions =
                reportDefinitionService.getActiveParams(reportMaster.getId());

        String executionId = UUID.randomUUID().toString();

        Map<String, Object> safeParams =
                requestParams != null ? requestParams : Map.of();

        try {
            List<Map<String, Object>> inputRows =
                    reportInputBindBuilder.buildRows(
                            reportMaster,
                            executionId,
                            safeParams,
                            paramDefinitions
                    );

            dynamicTableInsertService.insertBatch(
                    reportMaster.resolveInputTableName(),
                    inputRows
            );

            preProcessExecutorResolver
                    .resolve(reportMaster.getPreProcessType())
                    .execute(reportMaster, executionId);

            return ReportExecuteResponse.builder()
                    .reportCode(reportCode)
                    .executionId(executionId)
                    .outputFormat(
                            reportMaster.getOutputFormat() != null
                                    ? reportMaster.getOutputFormat().name()
                                    : null
                    )
                    .fileName(reportMaster.getFileName())
                    .previewEnabled(reportMaster.getPreviewEnabled())
                    .message("帳票実行の準備が完了しました。")
                    .build();

        } catch (Exception e) {
            reportHistoryService.saveFailure(
                    reportMaster,
                    executionId,
                    safeParams,
                    e
            );
            throw e;
        }
    }
}