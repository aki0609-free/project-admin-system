package com.project.backend.features.system.imports.service.collector;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.imports.dto.ImportWriteError;
import com.project.backend.features.system.imports.dto.ImportWriteResult;

@Component
public class ImportWriteResultCollector {

    public static final String KEY = "importWriteResult";
    public static final int MAX_PERSISTED_ERRORS = 1000;

    public ImportWriteResult empty() {
        return ImportWriteResult.builder()
                .totalCount(0)
                .insertedCount(0)
                .updatedCount(0)
                .skippedCount(0)
                .errorCount(0)
                .errors(List.of())
                .build();
    }

    public ImportWriteResult merge(
            ImportWriteResult current,
            ImportWriteResult added
    ) {
        if (current == null) {
            current = empty();
        }

        if (added == null) {
            return current;
        }

        List<ImportWriteError> errors = new ArrayList<>();
        errors.addAll(current.errors() != null ? current.errors() : List.of());
        errors.addAll(added.errors() != null ? added.errors() : List.of());

        if (errors.size() > MAX_PERSISTED_ERRORS) {
            errors = new ArrayList<>(
                    errors.subList(
                            0,
                            MAX_PERSISTED_ERRORS
                    )
            );
        }

        return ImportWriteResult.builder()
                .totalCount(current.totalCount() + added.totalCount())
                .insertedCount(current.insertedCount() + added.insertedCount())
                .updatedCount(current.updatedCount() + added.updatedCount())
                .skippedCount(current.skippedCount() + added.skippedCount())
                .errorCount(current.errorCount() + added.errorCount())
                .errors(errors)
                .build();
    }
}
