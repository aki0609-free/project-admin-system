package com.project.backend.features.system.report.service.builder;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.entity.ReportMaster;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportFileNameBuilder {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9_]*)}");

    private final Clock clock;

    public String build(ReportMaster reportMaster, String extension) {
        return build(reportMaster, extension, List.of());
    }

    public String build(
            ReportMaster reportMaster,
            String extension,
            List<Map<String, Object>> rows
    ) {
        String baseName = StringUtils.hasText(reportMaster.getFileName())
                ? reportMaster.getFileName()
                : reportMaster.getReportCode();

        baseName = resolvePlaceholders(baseName, rows);

        String timestamp = LocalDateTime.now(clock)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        return baseName + "_" + timestamp + "." + extension;
    }

    private String resolvePlaceholders(
            String template,
            List<Map<String, Object>> rows
    ) {
        if (rows == null || rows.isEmpty()) {
            return template;
        }

        Map<String, Object> values = rows.getFirst();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder resolved = new StringBuilder();

        while (matcher.find()) {
            Object value = findValue(values, matcher.group(1));
            if (value == null) {
                continue;
            }
            matcher.appendReplacement(
                    resolved,
                    Matcher.quoteReplacement(formatValue(matcher.group(1), value))
            );
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    private Object findValue(Map<String, Object> values, String placeholder) {
        if (values.containsKey(placeholder)) {
            return values.get(placeholder);
        }

        String snakeCase = placeholder.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase();
        if (values.containsKey(snakeCase)) {
            return values.get(snakeCase);
        }

        return values.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(placeholder)
                        || entry.getKey().equalsIgnoreCase(snakeCase))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String formatValue(String placeholder, Object value) {
        if (value instanceof java.sql.Date date) {
            return formatDate(placeholder, date.toLocalDate());
        }
        if (value instanceof LocalDate date) {
            return formatDate(placeholder, date);
        }
        if (value instanceof TemporalAccessor temporal) {
            return DateTimeFormatter.ISO_DATE.format(temporal);
        }
        return String.valueOf(value);
    }

    private String formatDate(String placeholder, LocalDate date) {
        if (placeholder.toLowerCase().contains("month")) {
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return date.toString();
    }
}
