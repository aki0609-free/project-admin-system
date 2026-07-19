package com.project.backend.features.system.imports.service.converter;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.imports.dto.ImportColumnDefinition;
import com.project.backend.features.system.imports.enums.ImportDataType;

@Component
public class ImportValueConverter {

    public Object convert(
            String value,
            ImportColumnDefinition column
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        ImportDataType dataType = column.dataType();

        try {
            return switch (dataType) {
                case STRING -> value;
                case INTEGER -> Integer.valueOf(value);
                case LONG -> Long.valueOf(value);
                case DECIMAL -> new BigDecimal(value);
                case BOOLEAN -> toBoolean(value);
                case DATE -> toSqlDate(value, column.formatPattern());
                case DATETIME -> toSqlTimestamp(value, column.formatPattern());
            };
        } catch (Exception e) {
            throw new RuntimeException(
                    "値の変換に失敗しました。"
                            + " columnName=" + column.columnName()
                            + ", csvHeaderName=" + column.csvHeaderName()
                            + ", dataType=" + dataType
                            + ", value=" + value,
                    e
            );
        }
    }

    private Boolean toBoolean(String value) {
        String normalized = value.trim().toLowerCase();

        return switch (normalized) {
            case "true", "1", "yes", "y", "on" -> true;
            case "false", "0", "no", "n", "off" -> false;
            default -> throw new RuntimeException("BOOLEANに変換できません。 value=" + value);
        };
    }

    private Date toSqlDate(String value, String pattern) {
        if (pattern != null && !pattern.isBlank()) {
            LocalDate date = LocalDate.parse(
                    value,
                    DateTimeFormatter.ofPattern(pattern)
            );
            return Date.valueOf(date);
        }

        return Date.valueOf(value);
    }

    private Timestamp toSqlTimestamp(String value, String pattern) {
        if (pattern != null && !pattern.isBlank()) {
            LocalDateTime dateTime = LocalDateTime.parse(
                    value,
                    DateTimeFormatter.ofPattern(pattern)
            );
            return Timestamp.valueOf(dateTime);
        }

        return Timestamp.valueOf(value);
    }
}