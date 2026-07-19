package com.project.backend.common.util;

public class ApplicationNumberUtils {
    
        public static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        return Integer.valueOf(String.valueOf(value));
    }

}
