package com.project.backend.common.util;

import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public class ApplicationValidationUtils {

    public static void validateIdentifier(String value, String label, Pattern pattern) {
        if (!StringUtils.hasText(value) || !pattern.matcher(value).matches()) {
            throw new RuntimeException(label + " に使用できない文字が含まれています。 value=" + value);
        }
    }
}
