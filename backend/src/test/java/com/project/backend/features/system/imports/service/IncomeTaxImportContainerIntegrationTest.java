package com.project.backend.features.system.imports.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;

import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.provider.IncomeTaxDeductionDetailProvider;
import com.project.backend.features.system.imports.entity.ImportColumn;
import com.project.backend.features.system.imports.entity.ImportTarget;
import com.project.backend.features.system.imports.entity.ImportTargetCatalog;
import com.project.backend.features.system.imports.enums.ImportDataType;
import com.project.backend.features.system.imports.enums.ImportMode;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.enums.ImportSourceType;
import com.project.backend.features.system.imports.repository.ImportTargetCatalogRepository;
import com.project.backend.features.system.imports.repository.ImportTargetRepository;
import com.project.backend.features.tax.repository.IncomeTaxBracketRepository;
import com.project.backend.testsupport.ContainerIntegrationTest;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IncomeTaxImportContainerIntegrationTest
        extends ContainerIntegrationTest {

    private static final String TARGET_CODE = "IMPORT_INCOME_TAX_TEST";

    @Autowired
    private ImportExecutionService importExecutionService;

    @Autowired
    private ImportTargetRepository targetRepository;

    @Autowired
    private ImportTargetCatalogRepository catalogRepository;

    @Autowired
    private IncomeTaxBracketRepository incomeTaxRepository;

    @Autowired
    private IncomeTaxDeductionDetailProvider detailProvider;

    @BeforeEach
    void prepareImportDefinition() {
        if (catalogRepository
                .findByTableNameAndActiveFlagTrueAndDeletedAtIsNull(
                        "income_tax_table"
                ).isEmpty()) {
            ImportTargetCatalog catalog = new ImportTargetCatalog();
            catalog.setTableName("income_tax_table");
            catalog.setDisplayName("源泉徴収税額表");
            catalog.setTenantScopedFlag(false);
            catalog.setAllowDeleteInsertFlag(true);
            catalog.setActiveFlag(true);
            catalogRepository.saveAndFlush(catalog);
        }

        if (targetRepository
                .findByTargetCodeAndActiveFlagTrueAndDeletedAtIsNull(
                        TARGET_CODE
                ).isEmpty()) {
            ImportTarget target = new ImportTarget();
            target.setTargetCode(TARGET_CODE);
            target.setTargetName("源泉徴収税額表取込テスト");
            target.setTableName("income_tax_table");
            target.setSourceType(ImportSourceType.UPLOAD);
            target.setScriptType(ImportScriptType.NONE);
            target.setImportMode(ImportMode.UPSERT);
            target.setHeaderRowNumber(1);
            target.setDataStartRowNumber(2);
            target.setCharset("UTF-8");
            target.setDelimiter(",");
            target.setActiveFlag(true);
            target.addColumn(column("year", "year", 1, true));
            target.addColumn(column("min_salary", "minSalary", 2, true));
            target.addColumn(column("max_salary", "maxSalary", 3, true));
            target.addColumn(column("dependents", "dependents", 4, true));
            target.addColumn(column("tax_amount", "taxAmount", 5, false));
            targetRepository.saveAndFlush(target);
        }
    }

    @Test
    void Python変換後形式を取り込み所得税控除詳細へ反映する() {
        MockMultipartFile convertedCsv = new MockMultipartFile(
                "file",
                "income-tax-table-2026.csv",
                "text/csv",
                ("year,minSalary,maxSalary,dependents,taxAmount\n"
                        + "2026,105000,106999,0,170\n"
                        + "2026,105000,106999,1,0\n")
                        .getBytes(StandardCharsets.UTF_8)
        );

        var first = importExecutionService.executeUpload(
                TARGET_CODE,
                convertedCsv
        );
        assertThat(first.status()).isEqualTo("COMPLETED");
        assertThat(incomeTaxRepository
                .findByYearOrderByMinSalaryAscDependentsAsc(2026))
                .hasSize(2);

        MockMultipartFile correctedCsv = new MockMultipartFile(
                "file",
                "income-tax-table-2026-corrected.csv",
                "text/csv",
                ("year,minSalary,maxSalary,dependents,taxAmount\n"
                        + "2026,105000,106999,0,180\n")
                        .getBytes(StandardCharsets.UTF_8)
        );
        assertThat(importExecutionService.executeUpload(
                TARGET_CODE,
                correctedCsv
        ).status()).isEqualTo("COMPLETED");

        assertThat(incomeTaxRepository
                .findByYearOrderByMinSalaryAscDependentsAsc(2026))
                .hasSize(2)
                .anySatisfy(row -> {
                    assertThat(row.getDependents()).isZero();
                    assertThat(row.getTaxAmount()).isEqualTo(180);
                });
        assertThat(detailProvider.getDetails(new DeductionMaster()))
                .hasSize(2)
                .anySatisfy(detail -> {
                    assertThat(detail.detailType()).isEqualTo("INCOME_TAX");
                    assertThat(detail.values().get("taxAmount")).isEqualTo(180);
                });
    }

    private ImportColumn column(
            String columnName,
            String csvHeader,
            int order,
            boolean key
    ) {
        ImportColumn column = new ImportColumn();
        column.setColumnName(columnName);
        column.setCsvHeaderName(csvHeader);
        column.setDataType(ImportDataType.INTEGER);
        column.setRequiredFlag(true);
        column.setKeyFlag(key);
        column.setNullableFlag(false);
        column.setTrimFlag(true);
        column.setUpdatableFlag(!key);
        column.setOrderNo(order);
        return column;
    }
}
