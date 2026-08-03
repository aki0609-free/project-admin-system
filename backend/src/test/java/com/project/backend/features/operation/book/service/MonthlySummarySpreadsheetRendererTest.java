package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;

class MonthlySummarySpreadsheetRendererTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MonthlySummarySpreadsheetRenderer renderer;
    private ExcelBookMaster master;
    private JsonNode template;

    @BeforeEach
    void setUp() throws Exception {
        renderer = new MonthlySummarySpreadsheetRenderer(
                objectMapper
        );
        master = new ExcelBookMaster();
        master.setBookCode("MONTHLY_SUMMARY");
        master.setTemplateSheetName("TEMPLATE");
        try (var input = new ClassPathResource(
                "spreadsheet/monthly_summary_template.json"
        ).getInputStream()) {
            template = objectMapper.readTree(input);
        }
    }

    @Test
    void render_shouldPlaceMonthlyDataAndKeepFormulaStyle()
            throws Exception {
        JsonNode result = renderer.render(
                template,
                master,
                List.of(row(
                        "DAILY",
                        LocalDate.of(2026, 2, 3),
                        2,
                        "1.5",
                        "0.5",
                        1900,
                        18500,
                        2891,
                        3469,
                        30000
                )),
                "2026-02",
                Instant.parse("2026-02-28T00:00:00Z")
        );

        JsonNode sheet = result.path("Workbook")
                .path("sheets").get(0);
        JsonNode rows = sheet.path("rows");

        assertThat(sheet.path("name").asText()).isEqualTo("2026.02");
        assertThat(cell(rows, 0, 9).path("value").asText())
                .isEqualTo("2026年");
        assertThat(cell(rows, 0, 13).path("value").asText())
                .isEqualTo("2月分");
        assertThat(cell(rows, 6, 0).path("value").asText())
                .isEqualTo("顧客A");
        assertThat(cell(rows, 6, 0).path("style")
                .isObject()).isTrue();
        assertThat(cell(rows, 6, 12).path("value").asInt())
                .isEqualTo(2);
        assertThat(cell(rows, 6, 15).path("value").asInt())
                .isEqualTo(1900);
        assertThat(cell(rows, 6, 128).path("value").asInt())
                .isEqualTo(18500);
        assertThat(cell(rows, 7, 4).path("formula").asText())
                .isEqualTo("=DY7*E7");
        assertThat(cell(rows, 7, 4).has("value")).isFalse();
        assertThat(cell(rows, 90, 12).path("value").asInt())
                .isEqualTo(30000);
        assertThat(result.path("projectAdminMetadata")
                .path("layoutType").asText())
                .isEqualTo("MONTHLY_SUMMARY");
    }

    @Test
    void render_shouldRejectUnsupportedBillingUnit()
            throws Exception {
        assertThatThrownBy(() -> renderer.render(
                template,
                master,
                List.of(row(
                        "HOURLY",
                        LocalDate.of(2026, 2, 3),
                        1,
                        0,
                        0,
                        0,
                        1000,
                        0,
                        0,
                        1000
                )),
                "2026-02",
                Instant.parse("2026-02-28T00:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DAILY");
    }

    @Test
    void preserveManualInputs_shouldKeepSocialInsurance()
            throws Exception {
        JsonNode generated = renderer.render(
                template,
                master,
                List.of(),
                "2026-02",
                Instant.parse("2026-02-28T00:00:00Z")
        );
        JsonNode existing = objectMapper.readTree(
                """
                {
                  "Workbook": {
                    "sheets": [{
                      "name": "2026.02",
                      "rows": [{
                        "index": 89,
                        "cells": [{"index": 4, "value": 1234}]
                      }]
                    }]
                  }
                }
                """
        );

        renderer.preserveManualInputs(generated, existing);

        assertThat(cell(
                generated.path("Workbook")
                        .path("sheets").get(0).path("rows"),
                89,
                4
        ).path("value").asInt()).isEqualTo(1234);
    }

    private Map<String, Object> row(
            String billingUnit,
            LocalDate workDate,
            Object personCount,
            Object overtimeHours,
            Object nightWorkHours,
            Object otherAmount,
            Object baseUnitPrice,
            Object overtimeUnitPrice,
            Object nightUnitPrice,
            Object estimatedGrossPay
    ) {
        return Map.ofEntries(
                Map.entry("billing_unit", billingUnit),
                Map.entry("work_date", workDate),
                Map.entry("customer_name", "顧客A"),
                Map.entry("site_name", "現場A"),
                Map.entry("job_code", "WORKER"),
                Map.entry("job_name", "普通作業員"),
                Map.entry("site_role_code", "GENERAL"),
                Map.entry("site_role_name", "一般"),
                Map.entry("person_count", personCount),
                Map.entry("overtime_hours", overtimeHours),
                Map.entry("night_work_hours", nightWorkHours),
                Map.entry("other_amount", otherAmount),
                Map.entry("base_unit_price", baseUnitPrice),
                Map.entry("overtime_unit_price", overtimeUnitPrice),
                Map.entry("night_unit_price", nightUnitPrice),
                Map.entry(
                        "estimated_gross_pay_amount",
                        estimatedGrossPay
                )
        );
    }

    private JsonNode cell(
            JsonNode rows,
            int rowIndex,
            int columnIndex
    ) {
        for (JsonNode row : rows) {
            if (row.path("index").asInt() != rowIndex) {
                continue;
            }
            for (JsonNode cell : row.path("cells")) {
                if (cell.path("index").asInt() == columnIndex) {
                    return cell;
                }
            }
        }
        return objectMapper.missingNode();
    }
}
