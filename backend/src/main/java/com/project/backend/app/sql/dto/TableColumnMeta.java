package com.project.backend.app.sql.dto;

public record TableColumnMeta(
        String columnName,
        Integer ordinalPosition,
        String isNullable,
        String dataType,
        String columnDefault,
        String extra
) {
    public boolean isAutoIncrement() {
        return extra != null && extra.toLowerCase().contains("auto_increment");
    }

    public boolean isNullableColumn() {
        return "YES".equalsIgnoreCase(isNullable);
    }

    public boolean hasDefaultValue() {
        return columnDefault != null;
    }
}