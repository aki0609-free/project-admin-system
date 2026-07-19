package com.project.backend.features.system.imports.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.dto.ImportWriteError;
import com.project.backend.features.system.imports.dto.ImportWriteResult;
import com.project.backend.features.system.imports.entity.ImportErrorRow;
import com.project.backend.features.system.imports.entity.ImportHistory;
import com.project.backend.features.system.imports.enums.ImportHistoryStatus;
import com.project.backend.features.system.imports.repository.ImportHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportHistoryService {

    private final ImportHistoryRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSuccess(
            ImportTargetDefinition target,
            String fileName,
            Long jobExecutionId,
            ImportWriteResult result
    ) {
        ImportHistory history = buildBaseHistory(
                target,
                fileName,
                jobExecutionId,
                result
        );

        history.setStatus(resolveStatus(result));
        history.setErrorMessage(null);

        for (ImportWriteError error : result.errors()) {
            history.addErrorRow(toErrorRow(error));
        }

        repository.save(history);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailure(
            ImportTargetDefinition target,
            String fileName,
            Long jobExecutionId,
            Exception exception
    ) {
        ImportHistory history = new ImportHistory();

        history.setTargetCode(target != null ? target.targetCode() : "");
        history.setTargetName(target != null ? target.targetName() : null);
        history.setTableName(target != null ? target.tableName() : null);
        history.setSourceType(target != null ? target.sourceType() : null);
        history.setImportMode(target != null ? target.importMode() : null);
        history.setFileName(fileName);
        history.setJobExecutionId(jobExecutionId);

        history.setTotalCount(0);
        history.setInsertedCount(0);
        history.setUpdatedCount(0);
        history.setSkippedCount(0);
        history.setErrorCount(1);

        history.setStatus(ImportHistoryStatus.FAILED);
        history.setExecutedBy("system");
        history.setExecutedAt(Instant.now());
        history.setErrorMessage(limit(exception.getMessage(), 4000));

        repository.save(history);
    }

    private ImportHistory buildBaseHistory(
            ImportTargetDefinition target,
            String fileName,
            Long jobExecutionId,
            ImportWriteResult result
    ) {
        ImportHistory history = new ImportHistory();

        history.setTargetCode(target.targetCode());
        history.setTargetName(target.targetName());
        history.setTableName(target.tableName());
        history.setSourceType(target.sourceType());
        history.setImportMode(target.importMode());
        history.setFileName(fileName);
        history.setJobExecutionId(jobExecutionId);

        history.setTotalCount(result.totalCount());
        history.setInsertedCount(result.insertedCount());
        history.setUpdatedCount(result.updatedCount());
        history.setSkippedCount(result.skippedCount());
        history.setErrorCount(result.errorCount());

        history.setExecutedBy("system");
        history.setExecutedAt(Instant.now());

        return history;
    }

    private ImportHistoryStatus resolveStatus(ImportWriteResult result) {
        if (result.errorCount() == 0) {
            return ImportHistoryStatus.SUCCESS;
        }

        if (result.insertedCount() > 0 || result.updatedCount() > 0 || result.skippedCount() > 0) {
            return ImportHistoryStatus.PARTIAL_SUCCESS;
        }

        return ImportHistoryStatus.FAILED;
    }

    private ImportErrorRow toErrorRow(ImportWriteError error) {
        ImportErrorRow entity = new ImportErrorRow();

        entity.setRowNo(error.rowNo());
        entity.setCsvHeaderName(error.csvHeaderName());
        entity.setColumnName(error.columnName());
        entity.setRawValue(error.rawValue());
        entity.setErrorMessage(limit(error.errorMessage(), 4000));

        return entity;
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }

        return value.length() <= max
                ? value
                : value.substring(0, max);
    }
}