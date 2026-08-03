package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.service.DocumentStorageKeyResolver;
import com.project.backend.features.system.excelbook.dto.SpreadsheetTemplateSaveRequest;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalog;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalogColumn;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.entity.ExcelBookVariableMapping;
import com.project.backend.features.system.excelbook.enums.ExcelBookSourceType;
import com.project.backend.features.system.excelbook.repository.ExcelBookDataSourceCatalogRepository;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;
import com.project.backend.features.system.excelbook.service.SpreadsheetTemplateService;
import com.project.backend.testsupport.ContainerIntegrationTest;

class SpreadsheetLedgerGenerationIntegrationTest
        extends ContainerIntegrationTest {

    private static final String TENANT_ID = "ledger-it";
    private static final String OTHER_TENANT_ID = "ledger-other";
    private static final String BOOK_CODE = "LEDGER_INTEGRATION_TEST";
    private static final String SOURCE_CODE = "LEDGER_INTEGRATION_SOURCE";
    private static final String SOURCE_TABLE =
            "it_spreadsheet_ledger_source";
    private static final String SOURCE_VIEW =
            "vw_it_spreadsheet_ledger_source";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ExcelBookMasterRepository masterRepository;

    @Autowired
    private ExcelBookDataSourceCatalogRepository catalogRepository;

    @Autowired
    private SpreadsheetTemplateService templateService;

    @Autowired
    private SpreadsheetLedgerGenerationService generationService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private DocumentStorageKeyResolver storageKeyResolver;

    @Autowired
    private ObjectMapper objectMapper;

    private ExcelBookMaster master;
    private ExcelBookDataSourceCatalog catalog;
    private String generatedStorageKey;

    @BeforeEach
    void setUp() throws Exception {
        createSourceView();
        catalog = catalogRepository.saveAndFlush(createCatalog());
        master = masterRepository.saveAndFlush(createMaster());

        templateService.save(
                master.getId(),
                new SpreadsheetTemplateSaveRequest(template())
        );
    }

    @AfterEach
    void tearDown() {
        try {
            if (generatedStorageKey != null
                    && storageService.exists(generatedStorageKey)) {
                storageService.delete(generatedStorageKey);
            }

            if (master != null) {
                String templateKey = storageKeyResolver.resolve(
                        DocumentArea.TEMPLATES,
                        "ledgers/"
                                + TENANT_ID
                                + "/"
                                + BOOK_CODE
                                + "/template.json"
                );
                if (storageService.exists(templateKey)) {
                    storageService.delete(templateKey);
                }
                masterRepository.deleteById(master.getId());
                masterRepository.flush();
            }

            if (catalog != null) {
                catalogRepository.deleteById(catalog.getId());
                catalogRepository.flush();
            }
        } finally {
            TenantContext.clear();
            jdbcTemplate.execute(
                    "DROP VIEW IF EXISTS " + SOURCE_VIEW
            );
            jdbcTemplate.execute(
                    "DROP TABLE IF EXISTS " + SOURCE_TABLE
            );
        }
    }

    @Test
    void generate_shouldQueryTenantRowsExpandWorkbookAndSaveJson()
            throws Exception {
        TenantContext.setTenantId(TENANT_ID);

        var result = generationService.generate(
                BOOK_CODE,
                "2026-07"
        );

        assertThat(result.rowCount()).isEqualTo(2);
        assertThat(result.storagePath())
                .startsWith(
                        "ledgers/"
                                + TENANT_ID
                                + "/"
                                + BOOK_CODE
                                + "/2026-07/"
                )
                .endsWith(".json");

        JsonNode rows = result.workbook()
                .path("Workbook")
                .path("sheets")
                .get(0)
                .path("rows");

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).path("cells").get(0).path("value").asText())
                .isEqualTo("対象月: 2026-07");
        assertDetailRow(
                rows.get(1),
                "E001",
                "山田 太郎",
                new BigDecimal("1200.50"),
                "=C2*2"
        );
        assertDetailRow(
                rows.get(2),
                "E002",
                "佐藤 花子",
                new BigDecimal("980.00"),
                "=C3*2"
        );

        generatedStorageKey = storageKeyResolver.resolve(
                DocumentArea.GENERATED_REPORTS,
                result.storagePath()
        );
        assertThat(storageService.exists(generatedStorageKey)).isTrue();

        try (InputStream inputStream =
                     storageService.load(generatedStorageKey)) {
            JsonNode saved = objectMapper.readTree(inputStream);
            JsonNode savedRows = saved
                    .path("Workbook")
                    .path("sheets")
                    .get(0)
                    .path("rows");

            assertThat(saved.toString()).doesNotContain("${");
            assertThat(saved.path("Workbook").path("locale").asText())
                    .isEqualTo("ja");
            assertThat(savedRows).hasSize(3);
            assertDetailRow(
                    savedRows.get(1),
                    "E001",
                    "山田 太郎",
                    new BigDecimal("1200.50"),
                    "=C2*2"
            );
            assertDetailRow(
                    savedRows.get(2),
                    "E002",
                    "佐藤 花子",
                    new BigDecimal("980.00"),
                    "=C3*2"
            );
        }
    }

    private void createSourceView() {
        jdbcTemplate.execute(
                "DROP VIEW IF EXISTS " + SOURCE_VIEW
        );
        jdbcTemplate.execute(
                "DROP TABLE IF EXISTS " + SOURCE_TABLE
        );
        jdbcTemplate.execute("""
                CREATE TABLE it_spreadsheet_ledger_source (
                    tenant_id VARCHAR(255) NOT NULL,
                    target_month VARCHAR(7) NOT NULL,
                    employee_code VARCHAR(20) NOT NULL,
                    employee_name VARCHAR(100) NOT NULL,
                    amount DECIMAL(12, 2) NOT NULL
                )
                """);
        jdbcTemplate.update(
                """
                INSERT INTO it_spreadsheet_ledger_source (
                    tenant_id,
                    target_month,
                    employee_code,
                    employee_name,
                    amount
                ) VALUES (?, ?, ?, ?, ?)
                """,
                TENANT_ID,
                "2026-07",
                "E001",
                "山田 太郎",
                new BigDecimal("1200.50")
        );
        jdbcTemplate.update(
                """
                INSERT INTO it_spreadsheet_ledger_source (
                    tenant_id,
                    target_month,
                    employee_code,
                    employee_name,
                    amount
                ) VALUES (?, ?, ?, ?, ?)
                """,
                TENANT_ID,
                "2026-07",
                "E002",
                "佐藤 花子",
                new BigDecimal("980.00")
        );
        jdbcTemplate.update(
                """
                INSERT INTO it_spreadsheet_ledger_source (
                    tenant_id,
                    target_month,
                    employee_code,
                    employee_name,
                    amount
                ) VALUES (?, ?, ?, ?, ?)
                """,
                OTHER_TENANT_ID,
                "2026-07",
                "E999",
                "他テナント",
                new BigDecimal("9999.00")
        );
        jdbcTemplate.update(
                """
                INSERT INTO it_spreadsheet_ledger_source (
                    tenant_id,
                    target_month,
                    employee_code,
                    employee_name,
                    amount
                ) VALUES (?, ?, ?, ?, ?)
                """,
                TENANT_ID,
                "2026-06",
                "E000",
                "前月データ",
                new BigDecimal("100.00")
        );
        jdbcTemplate.execute("""
                CREATE VIEW vw_it_spreadsheet_ledger_source AS
                SELECT
                    tenant_id,
                    target_month,
                    employee_code,
                    employee_name,
                    amount
                FROM it_spreadsheet_ledger_source
                """);
    }

    private ExcelBookDataSourceCatalog createCatalog() {
        ExcelBookDataSourceCatalog entity =
                new ExcelBookDataSourceCatalog();
        entity.setSourceCode(SOURCE_CODE);
        entity.setDisplayName("台帳統合テスト");
        entity.setPhysicalName(SOURCE_VIEW);
        entity.setWhereClauseTemplate(
                "tenant_id = :tenantId "
                        + "AND target_month = :targetMonth"
        );
        entity.setTenantScopedFlag(true);
        entity.setMaxRows(100);
        entity.setDescription("Testcontainers専用");
        entity.setActiveFlag(true);
        entity.setTenantId(TENANT_ID);

        addCatalogColumn(
                entity,
                "employee_code",
                "従業員コード",
                "STRING",
                1
        );
        addCatalogColumn(
                entity,
                "employee_name",
                "従業員名",
                "STRING",
                2
        );
        addCatalogColumn(
                entity,
                "amount",
                "金額",
                "NUMBER",
                3
        );
        return entity;
    }

    private void addCatalogColumn(
            ExcelBookDataSourceCatalog parent,
            String columnName,
            String displayName,
            String dataType,
            int orderNo
    ) {
        ExcelBookDataSourceCatalogColumn column =
                new ExcelBookDataSourceCatalogColumn();
        column.setCatalog(parent);
        column.setColumnName(columnName);
        column.setDisplayName(displayName);
        column.setDataType(dataType);
        column.setOrderNo(orderNo);
        column.setActiveFlag(true);
        column.setTenantId(TENANT_ID);
        parent.getColumns().add(column);
    }

    private ExcelBookMaster createMaster() {
        ExcelBookMaster entity = new ExcelBookMaster();
        entity.setBookCode(BOOK_CODE);
        entity.setBookName("Spreadsheet台帳統合テスト");
        entity.setTemplateFilePath("");
        entity.setOutputFilePath("");
        entity.setSourceType(ExcelBookSourceType.SNAPSHOT);
        entity.setSourceName(SOURCE_CODE);
        entity.setTemplateSheetName("TEMPLATE");
        entity.setActiveFlag(true);
        entity.setTenantId(TENANT_ID);

        entity.addVariableMapping(mapping(
                "rows.employeeCode",
                "employee_code",
                "STRING",
                1
        ));
        entity.addVariableMapping(mapping(
                "rows.employeeName",
                "employee_name",
                "STRING",
                2
        ));
        entity.addVariableMapping(mapping(
                "rows.amount",
                "amount",
                "NUMBER",
                3
        ));
        return entity;
    }

    private ExcelBookVariableMapping mapping(
            String variableKey,
            String sourceColumn,
            String dataType,
            int orderNo
    ) {
        ExcelBookVariableMapping mapping =
                new ExcelBookVariableMapping();
        mapping.setVariableKey(variableKey);
        mapping.setSourceColumn(sourceColumn);
        mapping.setScope("ROW");
        mapping.setDataType(dataType);
        mapping.setOrderNo(orderNo);
        mapping.setTenantId(TENANT_ID);
        return mapping;
    }

    private JsonNode template() throws Exception {
        return objectMapper.readTree("""
                {
                  "Workbook": {
                    "locale": "en-US",
                    "rowCount": 3,
                    "sheets": [
                      {
                        "name": "TEMPLATE",
                        "rowCount": 3,
                        "usedRange": {
                          "rowIndex": 1,
                          "colIndex": 3
                        },
                        "rows": [
                          {
                            "cells": [
                              {
                                "value": "対象月: ${targetMonth}"
                              }
                            ]
                          },
                          {
                            "cells": [
                              {
                                "value": "${rows.employeeCode}"
                              },
                              {
                                "value": "${rows.employeeName}"
                              },
                              {
                                "value": "${rows.amount}"
                              },
                              {
                                "formula": "=C2*2"
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                }
                """);
    }

    private void assertDetailRow(
            JsonNode row,
            String employeeCode,
            String employeeName,
            BigDecimal amount,
            String formula
    ) {
        JsonNode cells = row.path("cells");

        assertThat(cells.get(0).path("value").asText())
                .isEqualTo(employeeCode);
        assertThat(cells.get(1).path("value").asText())
                .isEqualTo(employeeName);
        assertThat(cells.get(2).path("value").decimalValue())
                .isEqualByComparingTo(amount);
        assertThat(cells.get(3).path("formula").asText())
                .isEqualTo(formula);
    }
}
