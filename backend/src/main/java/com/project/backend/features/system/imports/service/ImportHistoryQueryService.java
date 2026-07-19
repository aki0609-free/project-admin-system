package com.project.backend.features.system.imports.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.imports.dto.ImportErrorRowResponse;
import com.project.backend.features.system.imports.dto.ImportHistoryResponse;
import com.project.backend.features.system.imports.repository.ImportErrorRowRepository;
import com.project.backend.features.system.imports.repository.ImportHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportHistoryQueryService {

    private final ImportHistoryRepository historyRepository;
    private final ImportErrorRowRepository errorRowRepository;

    @Transactional(readOnly = true)
    public List<ImportHistoryResponse> findAll() {

        return historyRepository
                .findByDeletedAtIsNullOrderByExecutedAtDesc()
                .stream()
                .map(history -> ImportHistoryResponse.builder()
                        .id(history.getId())
                        .targetCode(history.getTargetCode())
                        .targetName(history.getTargetName())
                        .tableName(history.getTableName())
                        .sourceType(history.getSourceType())
                        .importMode(history.getImportMode())
                        .fileName(history.getFileName())
                        .totalCount(history.getTotalCount())
                        .insertedCount(history.getInsertedCount())
                        .updatedCount(history.getUpdatedCount())
                        .skippedCount(history.getSkippedCount())
                        .errorCount(history.getErrorCount())
                        .status(history.getStatus())
                        .jobExecutionId(history.getJobExecutionId())
                        .executedBy(history.getExecutedBy())
                        .executedAt(history.getExecutedAt())
                        .errorMessage(history.getErrorMessage())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ImportErrorRowResponse> findErrors(Long historyId) {

        return errorRowRepository
                .findByHistoryIdOrderByRowNoAsc(historyId)
                .stream()
                .map(error -> ImportErrorRowResponse.builder()
                        .id(error.getId())
                        .rowNo(error.getRowNo())
                        .csvHeaderName(error.getCsvHeaderName())
                        .columnName(error.getColumnName())
                        .rawValue(error.getRawValue())
                        .errorMessage(error.getErrorMessage())
                        .build())
                .toList();
    }
}