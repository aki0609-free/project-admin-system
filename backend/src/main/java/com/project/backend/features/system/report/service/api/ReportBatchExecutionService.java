package com.project.backend.features.system.report.service.api;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.mail.dto.MailSendResult;
import com.project.backend.features.system.mail.service.MailSendService;
import com.project.backend.features.system.report.dto.FileExportResult;
import com.project.backend.features.system.report.dto.ReportExecuteResponse;
import com.project.backend.features.system.report.dto.ReportStoredFile;
import com.project.backend.features.system.report.dto.ReportRecipientGenerationSummary;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.service.builder.ReportBatchRequestParamsBuilder;
import com.project.backend.features.system.report.service.builder.ReportBatchResultMessageBuilder;
import com.project.backend.features.system.report.service.core.ReportDefinitionService;
import com.project.backend.features.system.report.service.core.ReportFileGenerationService;
import com.project.backend.features.system.report.service.core.ReportRecipientFileGenerationService;
import com.project.backend.features.system.report.service.core.ReportStorageService;
import com.project.backend.features.system.report.service.loader.ReportOutputRowLoader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportBatchExecutionService {

    private final ReportDefinitionService reportDefinitionService;
    private final ReportExecutionService reportExecutionService;
    private final ReportExportService reportExportService;
    private final ReportStorageService reportStorageService;
    private final ReportHistoryService reportHistoryService;
    private final ReportOutputRowLoader reportOutputRowLoader;
    private final ReportFileGenerationService reportFileGenerationService;
    private final ReportRecipientFileGenerationService recipientFileGenerationService;
    private final ReportCleanupService reportCleanupService;
    private final ReportMailQueueService reportMailQueueService;
    private final MailSendService mailSendService;
    private final ReportBatchRequestParamsBuilder requestParamsBuilder;
    private final ReportBatchResultMessageBuilder resultMessageBuilder;

    public BatchJobExecutionResult execute(String reportCode) {
        return execute(reportCode, Map.of());
    }

    @Transactional
    public BatchJobExecutionResult execute(
            String reportCode,
            Map<String, Object> params
    ) {
        Map<String, Object> safeParams =
                params != null ? params : Map.of();

        ReportMaster reportMaster =
                reportDefinitionService.getActiveReport(reportCode);

        ReportExecuteResponse prepared =
                reportExecutionService.prepareExecution(
                        reportCode,
                        safeParams
                );

        FileExportResult exportResult =
                reportExportService.export(
                        reportCode,
                        prepared.executionId()
                );

        ReportStoredFile storedFile = saveAndRecordHistory(
                reportMaster,
                prepared.executionId(),
                requestParamsBuilder.build(
                        reportCode,
                        prepared.executionId(),
                        safeParams
                ),
                exportResult
        );

        return BatchJobExecutionResult.builder()
                .message(resultMessageBuilder.buildExportMessage())
                .storageType(storedFile.storageType())
                .outputFileKey(storedFile.fileKey())
                .outputFileName(storedFile.fileName())
                .contentType(storedFile.contentType())
                .fileSize(storedFile.fileSize())
                .build();
    }

    public BatchJobExecutionResult executeWithMail(String reportCode) {
        return executeWithMail(reportCode, Map.of());
    }

    public BatchJobExecutionResult executeWithMail(
            String reportCode,
            Map<String, Object> params
    ) {
        Map<String, Object> safeParams =
                params != null ? params : Map.of();

        ReportMaster reportMaster =
                reportDefinitionService.getActiveReport(reportCode);

        ReportExecuteResponse prepared =
                reportExecutionService.prepareExecution(
                        reportCode,
                        safeParams
                );

        List<Map<String, Object>> rows = reportOutputRowLoader.loadRows(
                reportMaster,
                prepared.executionId()
        );

        FileExportResult exportResult = reportFileGenerationService.generate(
                reportMaster,
                prepared.executionId(),
                rows
        );

        ReportStoredFile storedFile = saveAndRecordHistory(
                reportMaster,
                prepared.executionId(),
                requestParamsBuilder.build(
                        reportCode,
                        prepared.executionId(),
                        safeParams
                ),
                exportResult
        );

        ReportRecipientGenerationSummary generationSummary =
                recipientFileGenerationService.generate(
                        reportMaster,
                        prepared.executionId(),
                        rows
                );

        List<Long> queueIds =
                reportMailQueueService.createMailQueuesByExecutionId(
                        prepared.executionId()
                );

        MailSendResult mailResult =
                mailSendService.activateAndSendByIds(queueIds);

        reportCleanupService.cleanup(
                reportMaster,
                prepared.executionId()
        );

        return BatchJobExecutionResult.builder()
                .message(resultMessageBuilder.buildMailMessage(
                        storedFile,
                        generationSummary,
                        queueIds.size(),
                        mailResult
                ))
                .storageType(storedFile.storageType())
                .outputFileKey(storedFile.fileKey())
                .outputFileName(storedFile.fileName())
                .contentType(storedFile.contentType())
                .fileSize(storedFile.fileSize())
                .build();
    }

    private ReportStoredFile saveAndRecordHistory(
            ReportMaster reportMaster,
            String executionId,
            Map<String, Object> requestParams,
            FileExportResult exportResult
    ) {
        ReportStoredFile storedFile = reportStorageService.save(
                reportMaster,
                executionId,
                exportResult
        );

        try {
            reportHistoryService.saveSuccess(
                    reportMaster,
                    executionId,
                    requestParams,
                    storedFile
            );
            return storedFile;
        } catch (Exception e) {
            compensateStoredFile(storedFile, e);
            throw e;
        }
    }

    private void compensateStoredFile(
            ReportStoredFile storedFile,
            Exception originalException
    ) {
        try {
            reportStorageService.delete(
                    storedFile.storageType(),
                    storedFile.fileKey()
            );
        } catch (Exception deleteException) {
            originalException.addSuppressed(deleteException);
        }
    }

}
