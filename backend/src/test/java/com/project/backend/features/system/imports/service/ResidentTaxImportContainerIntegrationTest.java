package com.project.backend.features.system.imports.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;

import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.provider.ResidentTaxDeductionDetailProvider;
import com.project.backend.features.system.imports.entity.ImportColumn;
import com.project.backend.features.system.imports.entity.ImportTarget;
import com.project.backend.features.system.imports.entity.ImportTargetCatalog;
import com.project.backend.features.system.imports.enums.ImportDataType;
import com.project.backend.features.system.imports.enums.ImportMode;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.enums.ImportSourceType;
import com.project.backend.features.system.imports.repository.ImportTargetCatalogRepository;
import com.project.backend.features.system.imports.repository.ImportTargetRepository;
import com.project.backend.features.tax.repository.ResidentTaxMonthlyRepository;
import com.project.backend.testsupport.ContainerIntegrationTest;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ResidentTaxImportContainerIntegrationTest
        extends ContainerIntegrationTest {

    private static final String TARGET_CODE = "IMPORT_RESIDENT_TAX_TEST";

    @Autowired
    private ImportExecutionService importExecutionService;

    @Autowired
    private ImportTargetRepository targetRepository;

    @Autowired
    private ImportTargetCatalogRepository catalogRepository;

    @Autowired
    private ResidentTaxMonthlyRepository residentTaxRepository;

    @Autowired
    private ResidentTaxDeductionDetailProvider detailProvider;

    @Autowired
    private StorageProperties storageProperties;

    @BeforeEach
    void prepareImportDefinitionAndPythonScript() throws Exception {
        Path storageRoot = Path.of(storageProperties.getLocalBasePath())
                .toAbsolutePath()
                .normalize();
        Path scriptDirectory = storageRoot.resolve(
                storageProperties.getImports().getScript().getPath()
        );
        Files.createDirectories(scriptDirectory);
        Files.createDirectories(storageRoot.resolve(
                storageProperties.getImports().getCsv().getPath()
        ));
        Files.copy(
                new ClassPathResource(
                        "imports/scripts/convert_resident_tax.py"
                ).getInputStream(),
                scriptDirectory.resolve("convert_resident_tax.py"),
                StandardCopyOption.REPLACE_EXISTING
        );

        if (catalogRepository
                .findByTableNameAndActiveFlagTrueAndDeletedAtIsNull(
                        "resident_tax_monthly"
                ).isEmpty()) {
            ImportTargetCatalog catalog = new ImportTargetCatalog();
            catalog.setTableName("resident_tax_monthly");
            catalog.setDisplayName("住民税月額");
            catalog.setTenantScopedFlag(true);
            catalog.setAllowDeleteInsertFlag(false);
            catalog.setActiveFlag(true);
            catalogRepository.saveAndFlush(catalog);
        }

        if (targetRepository
                .findByTargetCodeAndActiveFlagTrueAndDeletedAtIsNull(
                        TARGET_CODE
                ).isEmpty()) {
            ImportTarget target = new ImportTarget();
            target.setTargetCode(TARGET_CODE);
            target.setTargetName("住民税通知CSV取込テスト");
            target.setTableName("resident_tax_monthly");
            target.setSourceType(ImportSourceType.UPLOAD);
            target.setFixedFilePath("resident_tax_test.csv");
            target.setScriptType(ImportScriptType.PYTHON);
            target.setScriptPath("convert_resident_tax.py");
            target.setScriptArgs(
                    "--year 2026 --input ${IMPORT_INPUT_FILE} "
                            + "--output ${IMPORT_CSV_DIR}/resident_tax_test.csv"
            );
            target.setImportMode(ImportMode.UPSERT);
            target.setHeaderRowNumber(1);
            target.setDataStartRowNumber(2);
            target.setCharset("UTF-8");
            target.setDelimiter(",");
            target.setActiveFlag(true);
            target.addColumn(column(
                    "employee_id", "employeeId", ImportDataType.LONG, true, 1
            ));
            target.addColumn(column(
                    "fiscal_year", "fiscalYear", ImportDataType.INTEGER, true, 2
            ));
            target.addColumn(column(
                    "month", "month", ImportDataType.INTEGER, true, 3
            ));
            target.addColumn(column(
                    "tax_amount", "taxAmount", ImportDataType.INTEGER, false, 4
            ));
            targetRepository.saveAndFlush(target);
        }
    }

    @Test
    void Pythonで住民税通知を月別化し控除詳細へ反映する() {
        MockMultipartFile notice = new MockMultipartFile(
                "file",
                "resident-tax-notice-2026.csv",
                "text/csv",
                "社員ID,6月,7月以降\n901,12000,11000\n"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        var result = importExecutionService.executeUpload(
                TARGET_CODE,
                notice
        );

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(residentTaxRepository
                .findByEmployeeIdAndFiscalYearOrderByMonthAsc(901L, 2026))
                .hasSize(12);
        assertThat(residentTaxRepository
                .findByEmployeeIdAndFiscalYearAndMonth(901L, 2026, 6))
                .get()
                .extracting(value -> value.getTaxAmount())
                .isEqualTo(12000);
        assertThat(residentTaxRepository
                .findByEmployeeIdAndFiscalYearAndMonth(901L, 2026, 7))
                .get()
                .extracting(value -> value.getTaxAmount())
                .isEqualTo(11000);

        testClock.setDate(LocalDate.of(2026, 7, 1));
        assertThat(detailProvider.getDetails(
                new DeductionMaster(),
                LocalDate.of(2026, 7, 1)
        ))
                .hasSize(12)
                .anySatisfy(detail -> {
                    assertThat(detail.detailType()).isEqualTo("RESIDENT_TAX");
                    assertThat(detail.values().get("employeeId")).isEqualTo(901L);
                    assertThat(detail.values().get("month")).isEqualTo(6);
                    assertThat(detail.values().get("taxAmount")).isEqualTo(12000);
                });
    }

    private ImportColumn column(
            String columnName,
            String csvHeader,
            ImportDataType dataType,
            boolean key,
            int order
    ) {
        ImportColumn column = new ImportColumn();
        column.setColumnName(columnName);
        column.setCsvHeaderName(csvHeader);
        column.setDataType(dataType);
        column.setRequiredFlag(true);
        column.setKeyFlag(key);
        column.setNullableFlag(false);
        column.setTrimFlag(true);
        column.setUpdatableFlag(!key);
        column.setOrderNo(order);
        return column;
    }
}
