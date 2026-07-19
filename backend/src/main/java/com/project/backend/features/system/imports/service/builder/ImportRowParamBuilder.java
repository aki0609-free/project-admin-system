package com.project.backend.features.system.imports.service.builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.imports.dto.ImportColumnDefinition;
import com.project.backend.features.system.imports.service.converter.ImportValueConverter;
import com.project.backend.features.system.imports.service.validation.ImportRowValidator;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ImportRowParamBuilder {

    private final ImportRowValidator rowValidator;
    private final ImportValueConverter valueConverter;

    public Map<String, Object> build(
            Map<String, String> row,
            List<ImportColumnDefinition> columns
    ) {
        Map<String, Object> params = new LinkedHashMap<>();

        for (ImportColumnDefinition column : columns) {

            String rawValue =
                    row.get(column.csvHeaderName());

            String normalizedValue =
                    rowValidator.normalizeAndValidate(
                            rawValue,
                            column
                    );

            Object convertedValue =
                    valueConverter.convert(
                            normalizedValue,
                            column
                    );

            params.put(
                    column.columnName(),
                    convertedValue
            );
        }

        return params;
    }
}