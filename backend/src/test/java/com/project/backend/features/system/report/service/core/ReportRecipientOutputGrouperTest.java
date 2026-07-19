package com.project.backend.features.system.report.service.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.report.dto.ReportRecipientOutputGroup;

class ReportRecipientOutputGrouperTest {

    private final ReportRecipientOutputGrouper grouper =
            new ReportRecipientOutputGrouper();

    @Test
    void group_shouldGroupMultipleRowsByBusinessKey() {
        List<Map<String, Object>> rows = List.of(
                row("MONTHLY:2026-07:1", "1", "one@example.com", 100),
                row("MONTHLY:2026-07:1", "1", "one@example.com", 200),
                row("MONTHLY:2026-07:2", "2", "two@example.com", 300)
        );

        List<ReportRecipientOutputGroup> groups = grouper.group(rows);

        assertThat(groups).hasSize(2);
        assertThat(groups.get(0).businessKey()).isEqualTo("MONTHLY:2026-07:1");
        assertThat(groups.get(0).rows()).hasSize(2);
        assertThat(groups.get(1).businessKey()).isEqualTo("MONTHLY:2026-07:2");
        assertThat(groups.get(1).rows()).hasSize(1);
    }

    @Test
    void group_shouldAllowMissingRecipientEmail() {
        List<ReportRecipientOutputGroup> groups = grouper.group(
                List.of(row("MONTHLY:2026-07:1", "1", null, 100))
        );

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().recipientEmail()).isNull();
    }

    @Test
    void group_shouldRejectMissingBusinessKey() {
        Map<String, Object> row = row(null, "1", "one@example.com", 100);

        assertThatThrownBy(() -> grouper.group(List.of(row)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("business_key");
    }

    @Test
    void group_shouldRejectDifferentRecipientInSameBusinessKey() {
        List<Map<String, Object>> rows = List.of(
                row("MONTHLY:2026-07:1", "1", "one@example.com", 100),
                row("MONTHLY:2026-07:1", "2", "two@example.com", 200)
        );

        assertThatThrownBy(() -> grouper.group(rows))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("recipient_key");
    }

    @Test
    void group_shouldReadCamelCaseOutputColumns() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("businessKey", "DAILY:2026-07-14:1");
        row.put("recipientKey", "1");
        row.put("recipientName", "従業員1");
        row.put("recipientEmail", "one@example.com");
        row.put("mailType", "DAILY_PAY_SLIP");
        row.put("mailTemplateKey", "DAILY_PAY_SLIP_NOTICE");

        List<ReportRecipientOutputGroup> groups = grouper.group(List.of(row));

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().recipientKey()).isEqualTo("1");
    }

    private Map<String, Object> row(
            String businessKey,
            String recipientKey,
            String recipientEmail,
            int amount
    ) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("business_key", businessKey);
        row.put("recipient_key", recipientKey);
        row.put("recipient_name", "従業員" + recipientKey);
        row.put("recipient_email", recipientEmail);
        row.put("mail_type", "MONTHLY_PAY_SLIP");
        row.put("mail_template_key", "MONTHLY_PAY_SLIP_NOTICE");
        row.put("amount", amount);
        return row;
    }
}
