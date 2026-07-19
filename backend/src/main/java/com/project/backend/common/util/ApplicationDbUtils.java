package com.project.backend.common.util;

import java.util.Map;

public class ApplicationDbUtils {

    public static String value(Map<String, Object> row, String key) {
        Object value = row.get(key);

        if (value == null) {
            value = row.get(ApplicationStringUtils.toCamelCase(key));
        }

        return value != null ? String.valueOf(value) : null;
    }
}
