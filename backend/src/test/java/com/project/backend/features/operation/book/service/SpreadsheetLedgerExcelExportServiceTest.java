package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class SpreadsheetLedgerExcelExportServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SpreadsheetLedgerExcelExportService service =
            new SpreadsheetLedgerExcelExportService();

    @Test
    void export_shouldPreserveSheetsValuesStylesAndMergedCells()
            throws Exception {
        var source = objectMapper.readTree("""
                {
                  "Workbook": {
                    "sheets": [{
                      "name": "日払い",
                      "showGridLines": false,
                      "frozenRows": 1,
                      "columns": [{"width": 90}, {"width": 120}],
                      "rows": [
                        {"height": 24, "cells": [{
                          "value": "労務費支払一覧",
                          "colSpan": 2,
                          "style": {
                            "fontWeight": "bold",
                            "backgroundColor": "#D9EAF7",
                            "textAlign": "Center",
                            "border": "1px solid #777777"
                          }
                        }]},
                        {"cells": [
                          {"value": "金額"},
                          {"value": 12345, "format": "#,##0"}
                        ]}
                      ]
                    }]
                  },
                  "projectAdminMetadata": {
                    "paperSize": "A4",
                    "orientation": "LANDSCAPE",
                    "fitToOnePage": true
                  }
                }
                """);

        byte[] result = service.export(source);

        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(result)
        )) {
            var sheet = workbook.getSheet("日払い");
            assertThat(sheet).isNotNull();
            assertThat(sheet.isDisplayGridlines()).isFalse();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("労務費支払一覧");
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue())
                    .isEqualTo(12345d);
            assertThat(sheet.getRow(0).getCell(0).getCellStyle()
                    .getFillPattern()).isEqualTo(
                            FillPatternType.SOLID_FOREGROUND
                    );
            assertThat(sheet.getMergedRegions()).containsExactly(
                    new CellRangeAddress(0, 0, 0, 1)
            );
            assertThat(sheet.getPrintSetup().getLandscape()).isTrue();
        }
    }
}
