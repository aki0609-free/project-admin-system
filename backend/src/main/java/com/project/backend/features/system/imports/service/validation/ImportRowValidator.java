package com.project.backend.features.system.imports.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.imports.dto.ImportColumnDefinition;

@Component
public class ImportRowValidator {

    public String normalizeAndValidate(
            String rawValue,
            ImportColumnDefinition column
    ) {
        String value = rawValue;

        if (column.trimFlag() && value != null) {
            value = value.trim();
        }

        if (!StringUtils.hasText(value)
                && StringUtils.hasText(column.defaultValue())) {
            value = column.defaultValue();
        }

        if (column.requiredFlag() && !StringUtils.hasText(value)) {
            throw new RuntimeException(
                    "必須項目が空です。 csvHeaderName=" + column.csvHeaderName()
            );
        }

        if (!column.nullableFlag() && !StringUtils.hasText(value)) {
            throw new RuntimeException(
                    "NULL不可項目が空です。 columnName=" + column.columnName()
            );
        }

        return value;
    }
}