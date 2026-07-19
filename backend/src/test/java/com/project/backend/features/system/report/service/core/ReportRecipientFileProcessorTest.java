package com.project.backend.features.system.report.service.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.system.report.dto.FileExportResult;
import com.project.backend.features.system.report.dto.ReportRecipientFileProcessResult;
import com.project.backend.features.system.report.dto.ReportRecipientOutputGroup;
import com.project.backend.features.system.report.dto.ReportStoredFile;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportOutputFile;
import com.project.backend.features.system.report.enums.ReportOutputFileStatus;
import com.project.backend.features.system.report.repository.ReportOutputFileRepository;
import com.project.backend.features.system.report.service.builder.ReportOutputFileBuilder;
import com.project.backend.features.system.report.service.validation.ReportOutputFileDuplicateChecker;

class ReportRecipientFileProcessorTest {

    private ReportFileGenerationService fileGenerationService;
    private ReportStorageService reportStorageService;
    private ReportOutputFileRepository repository;
    private ReportOutputFileDuplicateChecker duplicateChecker;
    private ReportRecipientFileProcessor processor;

    @BeforeEach
    void setUp() {
        fileGenerationService = mock(ReportFileGenerationService.class);
        reportStorageService = mock(ReportStorageService.class);
        repository = mock(ReportOutputFileRepository.class);
        duplicateChecker = mock(ReportOutputFileDuplicateChecker.class);
        processor = new ReportRecipientFileProcessor(
                fileGenerationService,
                reportStorageService,
                new ReportOutputFileBuilder(),
                repository,
                duplicateChecker
        );
    }

    @SuppressWarnings("null")
@Test
    void process_shouldStoreFailedOutputFile_whenRecipientEmailIsMissing() {
        ReportMaster reportMaster = reportMaster();
        ReportRecipientOutputGroup group = group(null);
        FileExportResult exportResult =
                new FileExportResult("report.pdf", "application/pdf", new byte[] {1});
        ReportStoredFile storedFile =
                new ReportStoredFile(StorageType.S3, "key", "report.pdf", "application/pdf", 1L);

        when(fileGenerationService.generate(reportMaster, "execution", group.rows()))
                .thenReturn(exportResult);
        when(reportStorageService.saveRecipient(
                reportMaster,
                "execution",
                group.businessKey(),
                exportResult
        )).thenReturn(storedFile);

        ReportRecipientFileProcessResult result = processor.process(
                reportMaster,
                "execution",
                group
        );

        ArgumentCaptor<ReportOutputFile> captor =
                ArgumentCaptor.forClass(ReportOutputFile.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReportOutputFileStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).contains("メールアドレス");
        assertThat(result.status()).isEqualTo(ReportOutputFileStatus.FAILED);
    }

    @SuppressWarnings("null")
@Test
    void process_shouldDeleteStoredFile_whenDatabaseSaveFails() {
        ReportMaster reportMaster = reportMaster();
        ReportRecipientOutputGroup group = group("one@example.com");
        FileExportResult exportResult =
                new FileExportResult("report.pdf", "application/pdf", new byte[] {1});
        ReportStoredFile storedFile =
                new ReportStoredFile(StorageType.S3, "key", "report.pdf", "application/pdf", 1L);

        when(fileGenerationService.generate(reportMaster, "execution", group.rows()))
                .thenReturn(exportResult);
        when(reportStorageService.saveRecipient(
                reportMaster,
                "execution",
                group.businessKey(),
                exportResult
        )).thenReturn(storedFile);
        when(repository.saveAndFlush(any(ReportOutputFile.class)))
                .thenThrow(new RuntimeException("database error"));

        assertThatThrownBy(() -> processor.process(reportMaster, "execution", group))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("database error");

        verify(reportStorageService).delete(StorageType.S3, "key");
    }

    @Test
    void process_shouldSkipBeforeGenerating_whenBusinessKeyIsDuplicate() {
        ReportMaster reportMaster = reportMaster();
        ReportRecipientOutputGroup group = group("one@example.com");
        when(duplicateChecker.isDuplicate(group.businessKey())).thenReturn(true);

        ReportRecipientFileProcessResult result = processor.process(
                reportMaster,
                "execution",
                group
        );

        assertThat(result.skipped()).isTrue();
        verify(fileGenerationService, never()).generate(any(), any(), any());
        verify(reportStorageService, never()).saveRecipient(any(), any(), any(), any());
    }

    private ReportMaster reportMaster() {
        ReportMaster reportMaster = new ReportMaster();
        reportMaster.setReportCode("MONTHLY_PAY_SLIP");
        return reportMaster;
    }

    private ReportRecipientOutputGroup group(String recipientEmail) {
        return new ReportRecipientOutputGroup(
                "MONTHLY:2026-07:1",
                "1",
                "従業員1",
                recipientEmail,
                null,
                "MONTHLY_PAY_SLIP",
                "MONTHLY_PAY_SLIP_NOTICE",
                List.of(Map.of("amount", 100))
        );
    }
}
