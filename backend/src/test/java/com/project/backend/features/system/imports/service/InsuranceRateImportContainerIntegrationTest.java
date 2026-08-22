package com.project.backend.features.system.imports.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;

import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.provider.EmploymentInsuranceDeductionDetailProvider;
import com.project.backend.features.master.deduction.provider.InsuranceRateDeductionDetailProvider;
import com.project.backend.features.system.imports.entity.ImportColumn;
import com.project.backend.features.system.imports.entity.ImportTarget;
import com.project.backend.features.system.imports.entity.ImportTargetCatalog;
import com.project.backend.features.system.imports.enums.ImportDataType;
import com.project.backend.features.system.imports.enums.ImportMode;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.enums.ImportSourceType;
import com.project.backend.features.system.imports.repository.ImportTargetCatalogRepository;
import com.project.backend.features.system.imports.repository.ImportTargetRepository;
import com.project.backend.features.tax.enums.InsuranceType;
import com.project.backend.features.tax.repository.InsuranceRateRepository;
import com.project.backend.testsupport.ContainerIntegrationTest;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InsuranceRateImportContainerIntegrationTest
        extends ContainerIntegrationTest {

    private static final String TARGET_CODE = "IMPORT_INSURANCE_RATE_TEST";

    @Autowired
    private ImportExecutionService importExecutionService;

    @Autowired
    private ImportTargetRepository targetRepository;

    @Autowired
    private ImportTargetCatalogRepository catalogRepository;

    @Autowired
    private InsuranceRateRepository insuranceRateRepository;

    @Autowired
    private InsuranceRateDeductionDetailProvider healthDetailProvider;

    @Autowired
    private EmploymentInsuranceDeductionDetailProvider employmentDetailProvider;

    @BeforeEach
    void prepareImportDefinition() {
        if (catalogRepository
                .findByTableNameAndActiveFlagTrueAndDeletedAtIsNull(
                        "insurance_rate_master"
                ).isEmpty()) {
            ImportTargetCatalog catalog = new ImportTargetCatalog();
            catalog.setTableName("insurance_rate_master");
            catalog.setDisplayName("社会保険料率");
            catalog.setTenantScopedFlag(false);
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
            target.setTargetName("社会保険料率取込テスト");
            target.setTableName("insurance_rate_master");
            target.setSourceType(ImportSourceType.UPLOAD);
            target.setScriptType(ImportScriptType.NONE);
            target.setImportMode(ImportMode.UPSERT);
            target.setHeaderRowNumber(1);
            target.setDataStartRowNumber(2);
            target.setCharset("UTF-8");
            target.setDelimiter(",");
            target.setActiveFlag(true);
            target.addColumn(column(
                    "insurance_type", "insuranceType",
                    ImportDataType.STRING, true, 1
            ));
            target.addColumn(column(
                    "year", "year", ImportDataType.INTEGER, true, 2
            ));
            target.addColumn(column(
                    "employee_rate", "employeeRate",
                    ImportDataType.DECIMAL, false, 3
            ));
            target.addColumn(column(
                    "employer_rate", "employerRate",
                    ImportDataType.DECIMAL, false, 4
            ));
            targetRepository.saveAndFlush(target);
        }
    }

    @Test
    void officialConvertedRatesImportAndAppearInDeductionDetails() {
        MockMultipartFile convertedRates = csv(
                "insurance-rates-2026.csv",
                "insuranceType,year,employeeRate,employerRate\n"
                        + "HEALTH_INSURANCE,2026,0.04805,0.04805\n"
                        + "CARE_INSURANCE,2026,0.00810,0.00810\n"
                        + "PENSION,2026,0.09150,0.09150\n"
                        + "EMPLOYMENT_INSURANCE,2026,0.00600,0.01050\n"
                        + "CHILD_CARE_SUPPORT,2026,0.00115,0.00115\n"
        );

        assertThat(importExecutionService.executeUpload(
                TARGET_CODE,
                convertedRates
        ).status()).isEqualTo("COMPLETED");

        assertThat(insuranceRateRepository.findByYearOrderByInsuranceTypeAsc(2026))
                .hasSize(5);
        assertRate(InsuranceType.HEALTH_INSURANCE, "0.04805");
        assertRate(InsuranceType.CARE_INSURANCE, "0.00810");
        assertRate(InsuranceType.PENSION, "0.09150");
        assertRate(InsuranceType.EMPLOYMENT_INSURANCE, "0.00600");
        assertRate(InsuranceType.CHILD_CARE_SUPPORT, "0.00115");

        testClock.setDate(LocalDate.of(2026, 8, 1));
        assertThat(healthDetailProvider.getDetails(
                new DeductionMaster(),
                LocalDate.of(2026, 8, 1)
        ))
                .singleElement()
                .satisfies(detail -> assertThat(
                        detail.values().get("employeeRate")
                ).isEqualTo(new BigDecimal("0.04805")));
        assertThat(employmentDetailProvider.getDetails(
                new DeductionMaster(),
                LocalDate.of(2026, 8, 1)
        ))
                .singleElement()
                .satisfies(detail -> assertThat(
                        detail.values().get("employeeRate")
                ).isEqualTo(new BigDecimal("0.00600")));

        MockMultipartFile correctedHealthRate = csv(
                "health-rate-corrected.csv",
                "insuranceType,year,employeeRate,employerRate\n"
                        + "HEALTH_INSURANCE,2026,0.04810,0.04810\n"
        );
        assertThat(importExecutionService.executeUpload(
                TARGET_CODE,
                correctedHealthRate
        ).status()).isEqualTo("COMPLETED");
        assertRate(InsuranceType.HEALTH_INSURANCE, "0.04810");
        assertThat(insuranceRateRepository.findByYearOrderByInsuranceTypeAsc(2026))
                .hasSize(5);
    }

    private void assertRate(InsuranceType type, String expected) {
        assertThat(insuranceRateRepository
                .findByInsuranceTypeAndYearOrderByIdAsc(type, 2026))
                .singleElement()
                .extracting(value -> value.getEmployeeRate())
                .isEqualTo(new BigDecimal(expected));
    }

    private MockMultipartFile csv(String name, String body) {
        return new MockMultipartFile(
                "file",
                name,
                "text/csv",
                body.getBytes(StandardCharsets.UTF_8)
        );
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
