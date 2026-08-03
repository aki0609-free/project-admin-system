package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.entity.ExcelBookVariableMapping;

class SpreadsheetWorkbookTemplateExpanderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SpreadsheetWorkbookTemplateExpander expander;
    private ExcelBookMaster master;

    @BeforeEach
    void setUp() {
        expander = new SpreadsheetWorkbookTemplateExpander(
                objectMapper
        );
        master = new ExcelBookMaster();
        master.setBookCode("EMPLOYEE_LEDGER");
        master.setBookName("従業員台帳");
        master.addVariableMapping(mapping(
                "rows.employeeName",
                "employee_name",
                "ROW",
                "STRING"
        ));
        master.addVariableMapping(mapping(
                "rows.salary",
                "salary",
                "ROW",
                "NUMBER"
        ));
    }

    @Test
    void expand_shouldRepeatDetailRowAndShiftRelativeFormula()
            throws Exception {
        JsonNode template = objectMapper.readTree(
                """
                {
                  "Workbook": {
                    "locale": "en-US",
                    "sheets": [{
                      "name": "TEMPLATE",
                      "rowCount": 100,
                      "usedRange": {"rowIndex": 1, "colIndex": 2},
                      "rows": [
                        {"cells": [
                          {"value": "対象月"},
                          {"value": "${targetMonth}"}
                        ]},
                        {"cells": [
                          {"value": "${rows.employeeName}"},
                          {"value": "${rows.salary}"},
                          {"formula": "=B2*$D$1", "value": "0"}
                        ]}
                      ]
                    }]
                  }
                }
                """
        );

        JsonNode result = expander.expand(
                template,
                master,
                List.of(
                        Map.of(
                                "employee_name", "山田 太郎",
                                "salary", 300_000
                        ),
                        Map.of(
                                "employee_name", "佐藤 花子",
                                "salary", 280_000
                        )
                ),
                "2026-07",
                Instant.parse("2026-07-28T12:00:00Z")
        );

        JsonNode workbook = result.path("Workbook");
        JsonNode rows = workbook.path("sheets").get(0).path("rows");

        assertThat(workbook.path("locale").asText()).isEqualTo("ja");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).path("cells").get(1).path("value").asText())
                .isEqualTo("2026-07");
        assertThat(rows.get(1).path("cells").get(0).path("value").asText())
                .isEqualTo("山田 太郎");
        assertThat(rows.get(1).path("cells").get(1).path("value").asInt())
                .isEqualTo(300_000);
        assertThat(rows.get(1).path("cells").get(2).path("formula").asText())
                .isEqualTo("=B2*$D$1");
        assertThat(rows.get(2).path("cells").get(0).path("value").asText())
                .isEqualTo("佐藤 花子");
        assertThat(rows.get(2).path("cells").get(2).path("formula").asText())
                .isEqualTo("=B3*$D$1");
        assertThat(rows.get(2).path("cells").get(2).has("value"))
                .isFalse();
        assertThat(workbook.path("sheets").get(0)
                .path("usedRange").path("rowIndex").asInt())
                .isEqualTo(2);
    }

    @Test
    void expand_shouldRejectUnregisteredVariable() throws Exception {
        JsonNode template = objectMapper.readTree(
                """
                {
                  "sheets": [{
                    "rows": [{"cells": [{"value": "${unknown}"}]}]
                  }]
                }
                """
        );

        assertThatThrownBy(() -> expander.expand(
                template,
                master,
                List.of(Map.of(
                        "employee_name", "山田 太郎",
                        "salary", 300_000
                )),
                "2026-07",
                Instant.parse("2026-07-28T12:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void expand_shouldRejectMultipleRepeatRowsInOneSheet()
            throws Exception {
        JsonNode template = objectMapper.readTree(
                """
                {
                  "sheets": [{
                    "rows": [
                      {"cells": [{"value": "${rows.employeeName}"}]},
                      {"cells": [{"value": "${rows.salary}"}]}
                    ]
                  }]
                }
                """
        );

        assertThatThrownBy(() -> expander.expand(
                template,
                master,
                List.of(Map.of(
                        "employee_name", "山田 太郎",
                        "salary", 300_000
                )),
                "2026-07",
                Instant.parse("2026-07-28T12:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1シート");
    }

    private ExcelBookVariableMapping mapping(
            String key,
            String column,
            String scope,
            String dataType
    ) {
        ExcelBookVariableMapping mapping =
                new ExcelBookVariableMapping();
        mapping.setVariableKey(key);
        mapping.setSourceColumn(column);
        mapping.setScope(scope);
        mapping.setDataType(dataType);
        mapping.setOrderNo(1);
        return mapping;
    }
}
