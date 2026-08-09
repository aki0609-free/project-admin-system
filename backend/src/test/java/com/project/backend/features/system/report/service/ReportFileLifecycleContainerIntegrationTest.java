package com.project.backend.features.system.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingReportFile;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingReportFileRepository;
import com.project.backend.features.operation.monthly.service.executor.MonthlyClosingJobExecutor;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.enums.BatchJobType;
import com.project.backend.features.system.batch.repository.BatchExecutionLogRepository;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;
import com.project.backend.features.system.report.entity.ReportHistory;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportParam;
import com.project.backend.features.system.report.enums.ReportCleanupType;
import com.project.backend.features.system.report.enums.ReportHistoryStatus;
import com.project.backend.features.system.report.enums.ReportOutputFormat;
import com.project.backend.features.system.report.enums.ReportParamControlType;
import com.project.backend.features.system.report.enums.ReportParamType;
import com.project.backend.features.system.report.enums.ReportPreProcessType;
import com.project.backend.features.system.report.repository.ReportHistoryRepository;
import com.project.backend.features.system.report.repository.ReportMasterRepository;
import com.project.backend.testsupport.ContainerIntegrationTest;

class ReportFileLifecycleContainerIntegrationTest
        extends ContainerIntegrationTest {

    private static final String REPORT_CODE = "IT_MONTHLY_REPORT_FILE";
    private static final String JOB_CODE = "IT_PRINT_MONTHLY_REPORT_FILE";
    private static final String INPUT_TABLE = "it_monthly_report_input";
    private static final String OUTPUT_TABLE = "it_monthly_report_output";
    private static final Long MONTHLY_CLOSING_ID = 91001L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReportMasterRepository reportMasterRepository;

    @Autowired
    private ReportHistoryRepository reportHistoryRepository;

    @Autowired
    private BatchJobDefinitionRepository batchJobDefinitionRepository;

    @Autowired
    private BatchExecutionLogRepository batchExecutionLogRepository;

    @Autowired
    private MonthlyClosingReportFileRepository reportFileRepository;

    @Autowired
    private MonthlyClosingJobExecutor monthlyClosingJobExecutor;

    @Autowired
    private StorageService storageService;

    private ReportMaster reportMaster;
    private BatchJobDefinition batchDefinition;
    private List<String> generatedFileKeys = List.of();

    @BeforeEach
    void setUpReportDefinition() {
        testClock.setInstant(Instant.parse("2026-08-31T06:00:00Z"));
        createWorkTables();
        reportMaster = reportMasterRepository.saveAndFlush(reportMaster());
        batchDefinition = batchJobDefinitionRepository.saveAndFlush(
                batchDefinition()
        );
    }

    @AfterEach
    void cleanUpReportDefinition() {
        generatedFileKeys.forEach(key -> {
            if (storageService.exists(StorageType.LOCAL, key)) {
                storageService.delete(StorageType.LOCAL, key);
            }
        });

        jdbcTemplate.update(
                "DELETE FROM monthly_closing_report_files "
                        + "WHERE monthly_closing_id = ?",
                MONTHLY_CLOSING_ID
        );
        jdbcTemplate.update(
                "DELETE FROM batch_execution_log WHERE job_code = ?",
                JOB_CODE
        );

        if (batchDefinition != null) {
            batchJobDefinitionRepository.deleteById(batchDefinition.getId());
            batchJobDefinitionRepository.flush();
        }
        if (reportMaster != null) {
            reportHistoryRepository
                    .findByReportMasterIdOrderByIdDesc(reportMaster.getId())
                    .forEach(reportHistoryRepository::delete);
            reportHistoryRepository.flush();
            reportMasterRepository.deleteById(reportMaster.getId());
            reportMasterRepository.flush();
        }

        jdbcTemplate.execute("DROP TABLE IF EXISTS " + OUTPUT_TABLE);
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + INPUT_TABLE);
    }

    @Test
    void initialAndReclosedReportsKeepBothFilesAndHistoryVersions()
            throws Exception {
        OperationReportPreview preview = preview();
        MonthlyClosingPeriod period = new MonthlyClosingPeriod(
                "2026-08",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                null
        );

        monthlyClosingJobExecutor.execute(
                MONTHLY_CLOSING_ID,
                preview,
                period,
                1
        );

        testClock.setInstant(Instant.parse("2026-09-02T06:00:00Z"));
        monthlyClosingJobExecutor.execute(
                MONTHLY_CLOSING_ID,
                preview,
                period,
                2
        );

        List<MonthlyClosingReportFile> versionOne = reportFileRepository
                .findAllByTargetMonthAndClosingVersionAndDeletedAtIsNullOrderByTargetNameAscIdAsc(
                        "2026-08",
                        1
                );
        List<MonthlyClosingReportFile> versionTwo = reportFileRepository
                .findAllByTargetMonthAndClosingVersionAndDeletedAtIsNullOrderByTargetNameAscIdAsc(
                        "2026-08",
                        2
                );
        assertThat(versionOne).hasSize(1);
        assertThat(versionTwo).hasSize(1);

        MonthlyClosingReportFile firstFile = versionOne.get(0);
        MonthlyClosingReportFile secondFile = versionTwo.get(0);
        generatedFileKeys = List.of(
                firstFile.getOutputFileKey(),
                secondFile.getOutputFileKey()
        );

        assertStoredCsv(firstFile, 1);
        assertStoredCsv(secondFile, 2);
        assertThat(secondFile.getOutputFileKey())
                .isNotEqualTo(firstFile.getOutputFileKey());
        assertThat(storageService.exists(
                firstFile.getStorageType(),
                firstFile.getOutputFileKey()
        )).isTrue();

        List<ReportHistory> histories = reportHistoryRepository
                .findByReportMasterIdOrderByIdDesc(reportMaster.getId());
        assertThat(histories).hasSize(2);
        assertThat(histories)
                .allSatisfy(history -> {
                    assertThat(history.getStatus())
                            .isEqualTo(ReportHistoryStatus.SUCCESS);
                    assertThat(history.getStorageType())
                            .isEqualTo(StorageType.LOCAL);
                    assertThat(history.getStoredFileKey())
                            .startsWith(
                                    "documents/generated-reports/reports/"
                                            + REPORT_CODE
                                            + "/"
                            );
                    assertThat(history.getFileSize()).isPositive();
                    assertThat(storageService.exists(
                            history.getStorageType(),
                            history.getStoredFileKey()
                    )).isTrue();
                });
        assertThat(histories)
                .extracting(ReportHistory::getStoredFileKey)
                .containsExactlyInAnyOrderElementsOf(generatedFileKeys);
    }

    private void assertStoredCsv(
            MonthlyClosingReportFile file,
            int closingVersion
    ) throws Exception {
        assertThat(file.getStorageType()).isEqualTo(StorageType.LOCAL);
        assertThat(file.getOutputFileKey())
                .startsWith(
                        "documents/generated-reports/reports/"
                                + REPORT_CODE
                                + "/"
                );
        assertThat(file.getOutputFileName()).endsWith(".csv");
        assertThat(file.getContentType()).startsWith("text/csv");
        assertThat(file.getFileSize()).isPositive();
        assertThat(storageService.exists(
                file.getStorageType(),
                file.getOutputFileKey()
        )).isTrue();

        try (InputStream input = storageService.load(
                file.getStorageType(),
                file.getOutputFileKey()
        )) {
            String csv = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
            assertThat(csv)
                    .contains("2026-08")
                    .contains(String.valueOf(closingVersion))
                    .contains("E001")
                    .contains("120000");
        }
    }

    private void createWorkTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + OUTPUT_TABLE);
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + INPUT_TABLE);
        jdbcTemplate.execute("""
                CREATE TABLE it_monthly_report_input (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    execution_id VARCHAR(100) NOT NULL,
                    tenant_id VARCHAR(255) NOT NULL,
                    target_month VARCHAR(7) NOT NULL,
                    closing_version BIGINT NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE it_monthly_report_output (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    execution_id VARCHAR(100) NOT NULL,
                    tenant_id VARCHAR(255) NOT NULL,
                    target_month VARCHAR(7) NOT NULL,
                    closing_version BIGINT NOT NULL,
                    employee_code VARCHAR(20) NOT NULL,
                    amount DECIMAL(12, 0) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL
                )
                """);
    }

    private ReportMaster reportMaster() {
        ReportMaster master = new ReportMaster();
        master.setReportCode(REPORT_CODE);
        master.setReportName("月次帳票ファイル統合テスト");
        master.setWorkTable("it_monthly_report");
        master.setInputTable(INPUT_TABLE);
        master.setOutputTable(OUTPUT_TABLE);
        master.setPreProcessType(ReportPreProcessType.SQL);
        master.setPreProcessSql("""
                INSERT INTO it_monthly_report_output (
                    execution_id,
                    tenant_id,
                    target_month,
                    closing_version,
                    employee_code,
                    amount,
                    created_at,
                    updated_at
                )
                SELECT
                    execution_id,
                    tenant_id,
                    target_month,
                    closing_version,
                    'E001',
                    120000,
                    created_at,
                    updated_at
                FROM it_monthly_report_input
                WHERE execution_id = :executionId
                """);
        master.setCleanupType(ReportCleanupType.NONE);
        master.setOutputFormat(ReportOutputFormat.CSV);
        master.setFileName("月次帳票統合テスト");
        master.setPreviewEnabled(true);
        master.setUseSignature(false);
        master.setActiveFlag(true);
        master.addParam(param(
                "targetMonth",
                "target_month",
                ReportParamType.STRING,
                ReportParamControlType.TEXT,
                1
        ));
        master.addParam(param(
                "closingVersion",
                "closing_version",
                ReportParamType.LONG,
                ReportParamControlType.NUMBER,
                2
        ));
        return master;
    }

    private ReportParam param(
            String name,
            String inputColumn,
            ReportParamType type,
            ReportParamControlType controlType,
            int order
    ) {
        ReportParam param = new ReportParam();
        param.setParamName(name);
        param.setParamLabel(name);
        param.setInputColumnName(inputColumn);
        param.setParamType(type);
        param.setControlType(controlType);
        param.setRequiredFlag(true);
        param.setVisibleFlag(true);
        param.setMultipleFlag(false);
        param.setFilterFlag(true);
        param.setDisplayOrder(order);
        param.setActiveFlag(true);
        return param;
    }

    private BatchJobDefinition batchDefinition() {
        BatchJobDefinition definition = new BatchJobDefinition();
        definition.setJobCode(JOB_CODE);
        definition.setJobName("月次帳票ファイル統合テスト");
        definition.setJobType(BatchJobType.REPORT);
        definition.setTargetCode(REPORT_CODE);
        definition.setImmediateExecutable(true);
        definition.setScheduleEnabled(false);
        definition.setActiveFlag(true);
        return definition;
    }

    private OperationReportPreview preview() {
        OperationReportPreview preview = new OperationReportPreview();
        preview.setOperationType(OperationType.MONTHLY);
        preview.setReportCode(REPORT_CODE);
        preview.setReportName("月次帳票ファイル統合テスト");
        preview.setJobCode(JOB_CODE);
        preview.setTableName(OUTPUT_TABLE);
        preview.setTemplateName("not-used-for-csv");
        preview.setOutputType(OperationReportOutputType.CSV);
        preview.setActiveFlag(true);
        return preview;
    }
}
