package com.project.backend.features.system.imports.service.builder;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.imports.dto.ImportWriteError;

@Component
public class ImportWriteErrorBuilder {

    public ImportWriteError build(
            int rowNo,
            Object rawValue,
            Exception e
    ) {
        return ImportWriteError.builder()
                .rowNo(rowNo)
                .csvHeaderName(null)
                .columnName(null)
                .rawValue(String.valueOf(rawValue))
                .errorMessage(limit(
                        e.getMessage(),
                        4000
                ))
                .build();
    }

    private String limit(
            String value,
            int max
    ) {
        if (value == null) {
            return null;
        }

        return value.length() <= max
                ? value
                : value.substring(0, max);
    }
}