package com.project.backend.features.system.report.service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.mail.dto.MailSendResult;
import com.project.backend.features.system.mail.service.MailSendService;
import com.project.backend.features.system.report.dto.FileExportResult;
import com.project.backend.features.system.report.dto.ReportExecuteResponse;
import com.project.backend.features.system.report.dto.ReportRecipientGenerationSummary;
import com.project.backend.features.system.report.dto.ReportStoredFile;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.service.builder.ReportBatchRequestParamsBuilder;
import com.project.backend.features.system.report.service.builder.ReportBatchResultMessageBuilder;
import com.project.backend.features.system.report.service.core.ReportDefinitionService;
import com.project.backend.features.system.report.service.core.ReportFileGenerationService;
import com.project.backend.features.system.report.service.core.ReportRecipientFileGenerationService;
import com.project.backend.features.system.report.service.core.ReportStorageService;
import com.project.backend.features.system.report.service.loader.ReportOutputRowLoader;

class ReportBatchExecutionServiceTest {

    @Test
    void executeWithMail_shouldGenerateCombinedAndRecipientFiles_thenSendEachMailType() {
        ReportDefinitionService definitionService = mock(ReportDefinitionService.class);
        ReportExecutionService executionService = mock(ReportExecutionService.class);
        ReportExportService exportService = mock(ReportExportService.class);
        ReportStorageService storageService = mock(ReportStorageService.class);
        ReportHistoryService historyService = mock(ReportHistoryService.class);
        ReportOutputRowLoader rowLoader = mock(ReportOutputRowLoader.class);
        ReportFileGenerationService fileGenerationService = mock(ReportFileGenerationService.class);
        ReportRecipientFileGenerationService recipientService = mock(ReportRecipientFileGenerationService.class);
        ReportCleanupService cleanupService = mock(ReportCleanupService.class);
        ReportMailQueueService queueService = mock(ReportMailQueueService.class);
        MailSendService mailSendService = mock(MailSendService.class);
        ReportBatchRequestParamsBuilder paramsBuilder = mock(ReportBatchRequestParamsBuilder.class);
        ReportBatchResultMessageBuilder messageBuilder = mock(ReportBatchResultMessageBuilder.class);

        ReportBatchExecutionService service = new ReportBatchExecutionService(
                definitionService,
                executionService,
                exportService,
                storageService,
                historyService,
                rowLoader,
                fileGenerationService,
                recipientService,
                cleanupService,
                queueService,
                mailSendService,
                paramsBuilder,
                messageBuilder
        );

        String reportCode = "MONTHLY_PAY_SLIP";
        String executionId = "execution-1";
        Map<String, Object> params = Map.of("targetMonth", "2026-07");
        Map<String, Object> requestParams = Map.of("executionId", executionId);
        List<Map<String, Object>> rows = List.of(Map.of("employee_id", 1L));
        ReportMaster master = new ReportMaster();
        FileExportResult export = new FileExportResult("all.pdf", "application/pdf", new byte[]{1});
        ReportStoredFile stored = new ReportStoredFile(
                StorageType.LOCAL,
                "reports-output/all.pdf",
                "all.pdf",
                "application/pdf",
                1L
        );
        ReportRecipientGenerationSummary summary = new ReportRecipientGenerationSummary(
                2,
                2,
                0,
                0,
                new LinkedHashSet<>(List.of("MONTHLY_PAY_SLIP", "OTHER_PAY_SLIP"))
        );

        when(definitionService.getActiveReport(reportCode)).thenReturn(master);
        when(executionService.prepareExecution(reportCode, params)).thenReturn(
                ReportExecuteResponse.builder().executionId(executionId).build()
        );
        when(rowLoader.loadRows(master, executionId)).thenReturn(rows);
        when(fileGenerationService.generate(master, executionId, rows)).thenReturn(export);
        when(paramsBuilder.build(reportCode, executionId, params)).thenReturn(requestParams);
        when(storageService.save(master, executionId, export)).thenReturn(stored);
        when(recipientService.generate(master, executionId, rows)).thenReturn(summary);
        List<Long> queueIds = List.of(10L, 11L);
        MailSendResult mailResult = MailSendResult.builder()
                .targetCount(2)
                .sentCount(1)
                .failedCount(1)
                .message("DRAFT有効化とメール送信が完了しました。")
                .build();
        when(queueService.createMailQueuesByExecutionId(executionId)).thenReturn(queueIds);
        when(mailSendService.activateAndSendByIds(queueIds)).thenReturn(mailResult);
        when(messageBuilder.buildMailMessage(stored, summary, 2, mailResult))
                .thenReturn("completed");

        BatchJobExecutionResult result = service.executeWithMail(reportCode, params);

        assertThat(result.message()).isEqualTo("completed");
        assertThat(result.outputFileKey()).isEqualTo(stored.fileKey());
        verify(historyService).saveSuccess(master, executionId, requestParams, stored);
        verify(recipientService).generate(master, executionId, rows);
        verify(queueService).createMailQueuesByExecutionId(executionId);
        verify(mailSendService).activateAndSendByIds(queueIds);
        verify(cleanupService).cleanup(master, executionId);
    }
}
