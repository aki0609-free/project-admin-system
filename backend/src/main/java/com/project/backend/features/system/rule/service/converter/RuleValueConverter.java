package com.project.backend.features.system.rule.service.converter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.rule.enums.RuleDataType;

@Component
public class RuleValueConverter {

    public Object convert(
            Object value,
            RuleDataType dataType,
            String valueName
    ) {
        if (value == null) {
            return null;
        }

        if (dataType == null) {
            throw new IllegalArgumentException(
                    valueName + "のdataTypeは必須です。"
            );
        }

        try {
            return switch (dataType) {
                case STRING -> String.valueOf(value);
                case INTEGER -> toInteger(value);
                case LONG -> toLong(value);
                case DECIMAL -> toDecimal(value);
                case BOOLEAN -> toBoolean(value);
                case DATE -> toDate(value);
                case DATETIME -> toDateTime(value);
            };
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    valueName
                            + "を"
                            + dataType
                            + "へ変換できません。 value="
                            + value,
                    exception
            );
        }
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value).trim());
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value).trim());
    }

    private BigDecimal toDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value).trim());
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }

        String text = String.valueOf(value).trim();

        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }

        throw new IllegalArgumentException(
                "BOOLEANはtrueまたはfalseで指定してください。"
        );
    }

    private LocalDate toDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        return LocalDate.parse(String.valueOf(value).trim());
    }

    private LocalDateTime toDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return LocalDateTime.parse(String.valueOf(value).trim());
    }
}
