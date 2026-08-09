package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.operation.monthly.dto.AnnualReportBackupResult;
import com.project.backend.features.operation.monthly.entity.AnnualReportBackupFile;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingReportFile;
import com.project.backend.features.operation.monthly.enums.AnnualReportBackupStatus;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.repository.AnnualReportBackupExecutionRepository;
import com.project.backend.features.operation.monthly.repository.AnnualReportBackupFileRepository;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingOutputDefinitionRepository;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingReportFileRepository;
import com.project.backend.features.system.report.entity.ReportHistory;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportHistoryStatus;
import com.project.backend.features.system.report.enums.ReportOutputFormat;
import com.project.backend.features.system.report.repository.ReportHistoryRepository;
import com.project.backend.features.system.report.repository.ReportMasterRepository;
import com.project.backend.testsupport.ContainerIntegrationTest;

class AnnualReportBackupContainerIntegrationTest
        extends ContainerIntegrationTest {

    private static final int FISCAL_YEAR = 2026;
    private static final String REPORT_CODE = "IT_ANNUAL_BACKUP_REPORT";
    private static final String OTHER_REPORT_CODE = "IT_NOT_BACKUP_REPORT";

    @Autowired
    private AnnualReportBackupService backupService;

    @Autowired
    private AnnualReportBackupExecutionRepository executionRepository;

    @Autowired
    private AnnualReportBackupFileRepository backupFileRepository;

    @Autowired
    private MonthlyClosingOutputDefinitionRepository definitionRepository;

    @Autowired
    private MonthlyClosingReportFileRepository reportFileRepository;

    @Autowired
    private ReportMasterRepository reportMasterRepository;

    @Autowired
    private ReportHistoryRepository reportHistoryRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> storageKeys = new ArrayList<>();
    private ReportMaster reportMaster;
    private MonthlyClosingOutputDefinition definition;

    @BeforeEach
    void setUpBackupTargets() {
        testClock.setInstant(Instant.parse("2027-04-15T00:00:00Z"));
        reportMaster = reportMasterRepository.saveAndFlush(reportMaster());
        definition = definitionRepository.saveAndFlush(definition());
    }

    @AfterEach
    void cleanUpBackupTargets() {
        storageKeys.forEach(key -> {
            if (storageService.exists(StorageType.LOCAL, key)) {
                storageService.delete(StorageType.LOCAL, key);
            }
        });
        jdbcTemplate.update(
                "DELETE FROM annual_report_backup_file WHERE tenant_id = ?",
                TEST_TENANT_ID
        );
        jdbcTemplate.update(
                "DELETE FROM annual_report_backup_execution WHERE tenant_id = ?",
                TEST_TENANT_ID
        );
        jdbcTemplate.update(
                "DELETE FROM monthly_closing_report_files "
                        + "WHERE report_code IN (?, ?)",
                REPORT_CODE,
                OTHER_REPORT_CODE
        );
        if (definition != null) {
            definitionRepository.deleteById(definition.getId());
            definitionRepository.flush();
        }
        if (reportMaster != null) {
            jdbcTemplate.update(
                    "DELETE FROM report_history WHERE report_master_id = ?",
                    reportMaster.getId()
            );
            reportMasterRepository.deleteById(reportMaster.getId());
            reportMasterRepository.flush();
        }
    }

    @Test
    void dueBackupCopiesEligibleFilesThenSoftDeletesOnlySourceHistory() {
        MonthlyClosingReportFile april = sourceFile(
                REPORT_CODE,
                "2026-04",
                1,
                "april.csv",
                "april"
        );
        MonthlyClosingReportFile marchReclosed = sourceFile(
                REPORT_CODE,
                "2027-03",
                2,
                "march-v2.csv",
                "march-v2"
        );
        MonthlyClosingReportFile excluded = sourceFile(
                OTHER_REPORT_CODE,
                "2026-08",
                1,
                "excluded.csv",
                "excluded"
        );
        saveHistory(april);
        saveHistory(marchReclosed);

        assertThat(backupService.findPendingFiscalYears(4, 14))
                .containsExactly(FISCAL_YEAR);

        AnnualReportBackupResult first = backupService.execute(
                FISCAL_YEAR,
                4,
                14
        );

        assertThat(first.status())
                .isEqualTo(AnnualReportBackupStatus.COMPLETED);
        assertThat(first.fileCount()).isEqualTo(2);
        assertThat(first.totalSize())
                .isEqualTo(april.getFileSize() + marchReclosed.getFileSize());

        List<AnnualReportBackupFile> backupFiles = backupFileRepository
                .findByTenantIdAndBackupExecutionIdAndDeletedAtIsNullOrderByIdAsc(
                        TEST_TENANT_ID,
                        first.executionId()
                );
        assertThat(backupFiles).hasSize(2);
        assertThat(backupFiles)
                .allSatisfy(file -> {
                    storageKeys.add(file.getBackupFileKey());
                    assertThat(file.getBackupFileKey())
                            .startsWith(
                                    "documents/backups/reports/"
                                            + TEST_TENANT_ID
                                            + "/2026/"
                            );
                    assertThat(file.getRetentionUntil().toString())
                            .isEqualTo("2034-03-31");
                    assertThat(storageService.exists(
                            file.getStorageType(),
                            file.getBackupFileKey()
                    )).isTrue();
                });

        assertThat(deletedAt("monthly_closing_report_files", april.getId()))
                .isNotNull();
        assertThat(deletedAt(
                "monthly_closing_report_files",
                marchReclosed.getId()
        )).isNotNull();
        assertThat(deletedAt(
                "monthly_closing_report_files",
                excluded.getId()
        )).isNull();
        assertThat(historyDeletedAt(april.getOutputFileKey())).isNotNull();
        assertThat(historyDeletedAt(marchReclosed.getOutputFileKey()))
                .isNotNull();

        assertThat(storageService.exists(
                StorageType.LOCAL,
                april.getOutputFileKey()
        )).isTrue();

        AnnualReportBackupResult second = backupService.execute(
                FISCAL_YEAR,
                4,
                14
        );
        assertThat(second.executionId()).isEqualTo(first.executionId());
        assertThat(second.fileCount()).isEqualTo(2);
        assertThat(backupFileRepository.count()).isEqualTo(2);
        assertThat(backupService.findPendingFiscalYears(4, 14)).isEmpty();
    }

    @Test
    void backupCannotRunBeforeGracePeriodHasElapsed() {
        testClock.setInstant(Instant.parse("2027-04-13T00:00:00Z"));

        assertThatThrownBy(() -> backupService.execute(
                FISCAL_YEAR,
                4,
                14
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2027-04-14");

        assertThat(executionRepository
                .findByTenantIdAndFiscalYearAndDeletedAtIsNull(
                        TEST_TENANT_ID,
                        FISCAL_YEAR
                )).isEmpty();
    }

    private MonthlyClosingReportFile sourceFile(
            String reportCode,
            String targetMonth,
            int closingVersion,
            String fileName,
            String content
    ) {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        String key = "documents/generated-reports/reports/"
                + reportCode
                + "/"
                + targetMonth
                + "/v"
                + closingVersion
                + "/"
                + fileName;
        storageService.save(
                StorageType.LOCAL,
                key,
                new ByteArrayInputStream(data),
                data.length,
                "text/csv"
        );
        storageKeys.add(key);

        MonthlyClosingReportFile file = new MonthlyClosingReportFile();
        file.setMonthlyClosingId(90000L + closingVersion);
        file.setTargetMonth(targetMonth);
        file.setClosingVersion(closingVersion);
        file.setReportCode(reportCode);
        file.setJobCode("PRINT_" + reportCode);
        file.setTargetType("ALL");
        file.setTargetName("全体");
        file.setStorageType(StorageType.LOCAL);
        file.setOutputFileKey(key);
        file.setOutputFileName(fileName);
        file.setContentType("text/csv");
        file.setFileSize((long) data.length);
        file.setGeneratedAt(Instant.now(testClock));
        return reportFileRepository.saveAndFlush(file);
    }

    private void saveHistory(MonthlyClosingReportFile source) {
        ReportHistory history = new ReportHistory();
        history.setReportMaster(reportMaster);
        history.setFileName(source.getOutputFileName());
        history.setOutputFormat(ReportOutputFormat.CSV);
        history.setStatus(ReportHistoryStatus.SUCCESS);
        history.setRequestParamsJson("{}");
        history.setExecutedBy("integration-test");
        history.setStorageType(source.getStorageType());
        history.setStoredFileKey(source.getOutputFileKey());
        history.setStoredFileName(source.getOutputFileName());
        history.setMimeType(source.getContentType());
        history.setFileSize(source.getFileSize());
        reportHistoryRepository.saveAndFlush(history);
    }

    private ReportMaster reportMaster() {
        ReportMaster master = new ReportMaster();
        master.setReportCode(REPORT_CODE);
        master.setReportName("年度バックアップ統合テスト帳票");
        master.setWorkTable("it_annual_backup_report");
        master.setOutputFormat(ReportOutputFormat.CSV);
        master.setFileName("annual-backup.csv");
        master.setActiveFlag(true);
        master.setPreviewEnabled(true);
        master.setUseSignature(false);
        return master;
    }

    private MonthlyClosingOutputDefinition definition() {
        MonthlyClosingOutputDefinition value =
                new MonthlyClosingOutputDefinition();
        value.setOutputType(MonthlyClosingOutputType.REPORT);
        value.setOutputCode(REPORT_CODE);
        value.setExecutionOrder(1);
        value.setRequiredFlag(true);
        value.setActiveFlag(true);
        value.setBackupRetentionYears(7);
        return value;
    }

    private Instant deletedAt(String tableName, Long id) {
        LocalDateTime value = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM " + tableName + " WHERE id = ?",
                LocalDateTime.class,
                id
        );
        return value != null ? value.toInstant(ZoneOffset.UTC) : null;
    }

    private Instant historyDeletedAt(String fileKey) {
        LocalDateTime value = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM report_history "
                        + "WHERE stored_file_key = ?",
                LocalDateTime.class,
                fileKey
        );
        return value != null ? value.toInstant(ZoneOffset.UTC) : null;
    }
}
