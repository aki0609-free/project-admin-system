package com.project.backend.common.util;

public class ApplicationStringUtils {

    public static String toCamelCase(String snake) {
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;

        for (char c : snake.toCharArray()) {
            if (c == '_') {
                upperNext = true;
                continue;
            }

            if (upperNext) {
                builder.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }
    
}
