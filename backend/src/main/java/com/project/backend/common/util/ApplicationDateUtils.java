package com.project.backend.common.util;

import java.time.LocalDate;

public class ApplicationDateUtils {

    public static LocalDate toLocalDate(Object value) {
        if (value == null)
            return null;

        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }

        return LocalDate.parse(String.valueOf(value));
    }
}
