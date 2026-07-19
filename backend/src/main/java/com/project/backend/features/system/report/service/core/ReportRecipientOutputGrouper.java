package com.project.backend.features.system.report.service.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.common.util.ApplicationStringUtils;
import com.project.backend.features.system.report.dto.ReportRecipientOutputGroup;

@Service
public class ReportRecipientOutputGrouper {

    public List<ReportRecipientOutputGroup> group(
            List<Map<String, Object>> rows
    ) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        Map<String, List<Map<String, Object>>> groupedRows =
                new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String businessKey = requiredValue(row, "business_key");
            groupedRows.computeIfAbsent(
                    businessKey,
                    ignored -> new ArrayList<>()
            ).add(row);
        }

        return groupedRows.entrySet()
                .stream()
                .map(entry -> buildGroup(entry.getKey(), entry.getValue()))
                .toList();
    }

    private ReportRecipientOutputGroup buildGroup(
            String businessKey,
            List<Map<String, Object>> rows
    ) {
        Map<String, Object> first = rows.getFirst();

        String recipientKey = requiredValue(first, "recipient_key");
        String recipientName = value(first, "recipient_name");
        String recipientEmail = value(first, "recipient_email");
        String recipientGroupKey = value(first, "recipient_group_key");
        String mailType = requiredValue(first, "mail_type");
        String mailTemplateKey = requiredValue(first, "mail_template_key");

        for (Map<String, Object> row : rows) {
            validateSame(businessKey, "recipient_key", recipientKey, value(row, "recipient_key"));
            validateSame(businessKey, "recipient_name", recipientName, value(row, "recipient_name"));
            validateSame(businessKey, "recipient_email", recipientEmail, value(row, "recipient_email"));
            validateSame(businessKey, "recipient_group_key", recipientGroupKey, value(row, "recipient_group_key"));
            validateSame(businessKey, "mail_type", mailType, value(row, "mail_type"));
            validateSame(businessKey, "mail_template_key", mailTemplateKey, value(row, "mail_template_key"));
        }

        return new ReportRecipientOutputGroup(
                businessKey,
                recipientKey,
                recipientName,
                recipientEmail,
                recipientGroupKey,
                mailType,
                mailTemplateKey,
                List.copyOf(rows)
        );
    }

    private void validateSame(
            String businessKey,
            String fieldName,
            String expected,
            String actual
    ) {
        if (!Objects.equals(expected, actual)) {
            throw new RuntimeException(
                    "同一businessKey内で宛先情報が一致しません。 field="
                            + fieldName
            );
        }
    }

    private String requiredValue(
            Map<String, Object> row,
            String key
    ) {
        String value = value(row, key);

        if (!StringUtils.hasText(value)) {
            throw new RuntimeException(
                    "REPORT_MAILの出力項目が未設定です。 key=" + key
            );
        }

        return value;
    }

    private String value(
            Map<String, Object> row,
            String key
    ) {
        Object rawValue = row.get(key);

        if (rawValue == null) {
            rawValue = row.get(ApplicationStringUtils.toCamelCase(key));
        }

        return rawValue != null
                ? String.valueOf(rawValue)
                : null;
    }
}
