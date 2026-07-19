package com.project.backend.common.util;

import java.util.List;

public class ApplicationCollectionUtils {

    public static List<?> toList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }

        if (value instanceof Object[] array) {
            return List.of(array);
        }

        return List.of(value);
    }
}
