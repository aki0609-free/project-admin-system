package com.project.backend.features.system.report.service.converter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.report.enums.ReportParamType;

@Component
public class ReportParamValueConverter {

    public Object convert(Object rawValue, ReportParamType paramType) {
        if (rawValue == null) {
            return null;
        }

        return switch (paramType) {
            case STRING -> String.valueOf(rawValue);
            case LONG -> toLong(rawValue);
            case DATE -> toLocalDate(rawValue);
            case BOOLEAN -> toBoolean(rawValue);
            case DECIMAL -> toBigDecimal(rawValue);
            case DATETIME -> toLocalDateTime(rawValue);
        };
    }

    private Long toLong(Object rawValue) {
        if (rawValue instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(String.valueOf(rawValue));
    }

    private Boolean toBoolean(Object rawValue) {
        if (rawValue instanceof Boolean b) {
            return b;
        }
        return Boolean.valueOf(String.valueOf(rawValue));
    }

    private BigDecimal toBigDecimal(Object rawValue) {
        if (rawValue instanceof BigDecimal bd) {
            return bd;
        }
        if (rawValue instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(String.valueOf(rawValue));
    }

    private LocalDate toLocalDate(Object rawValue) {
        if (rawValue instanceof LocalDate d) {
            return d;
        }
        return LocalDate.parse(String.valueOf(rawValue));
    }

    private LocalDateTime toLocalDateTime(Object rawValue) {
        if (rawValue instanceof LocalDateTime dt) {
            return dt;
        }
        return LocalDateTime.parse(String.valueOf(rawValue));
    }
}