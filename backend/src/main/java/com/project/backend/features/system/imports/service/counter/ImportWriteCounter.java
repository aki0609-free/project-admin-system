package com.project.backend.features.system.imports.service.counter;

import java.util.List;

import com.project.backend.features.system.imports.dto.ImportWriteError;
import com.project.backend.features.system.imports.dto.ImportWriteResult;

import lombok.Getter;

@Getter
public class ImportWriteCounter {

    private int totalCount;
    private int insertedCount;
    private int updatedCount;
    private int skippedCount;
    private int errorCount;

    public void countTotal() {
        totalCount++;
    }

    public void countInserted() {
        insertedCount++;
    }

    public void countUpdated(int count) {
        updatedCount += count;
    }

    public void countSkipped() {
        skippedCount++;
    }

    public void countError() {
        errorCount++;
    }

    public ImportWriteResult toResult(
            List<ImportWriteError> errors
    ) {
        return ImportWriteResult.builder()
                .totalCount(totalCount)
                .insertedCount(insertedCount)
                .updatedCount(updatedCount)
                .skippedCount(skippedCount)
                .errorCount(errorCount)
                .errors(errors)
                .build();
    }
}