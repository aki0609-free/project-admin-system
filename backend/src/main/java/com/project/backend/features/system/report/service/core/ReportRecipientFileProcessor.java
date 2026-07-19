package com.project.backend.features.system.report.service.core;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportRecipientFileProcessor {

    private final ReportFileGenerationService fileGenerationService;
    private final ReportStorageService reportStorageService;
    private final ReportOutputFileBuilder outputFileBuilder;
    private final ReportOutputFileRepository outputFileRepository;
    private final ReportOutputFileDuplicateChecker duplicateChecker;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReportRecipientFileProcessResult process(
            ReportMaster reportMaster,
            String executionId,
            ReportRecipientOutputGroup group
    ) {
        if (duplicateChecker.isDuplicate(group.businessKey())) {
            return ReportRecipientFileProcessResult.skippedResult();
        }

        FileExportResult exportResult = fileGenerationService.generate(
                reportMaster,
                executionId,
                group.rows()
        );

        ReportStoredFile storedFile = reportStorageService.saveRecipient(
                reportMaster,
                executionId,
                group.businessKey(),
                exportResult
        );

        try {
            ReportOutputFile outputFile = outputFileBuilder.build(
                    reportMaster,
                    executionId,
                    storedFile,
                    group
            );

            outputFileRepository.saveAndFlush(outputFile);

            if (outputFile.getStatus() == ReportOutputFileStatus.FAILED) {
                return ReportRecipientFileProcessResult.failed(group.mailType());
            }

            return ReportRecipientFileProcessResult.created(group.mailType());
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
