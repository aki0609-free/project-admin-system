package com.project.backend.features.operation.monthly.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.operation.monthly.dto.AnnualReportBackupResult;
import com.project.backend.features.operation.monthly.entity.AnnualReportBackupExecution;
import com.project.backend.features.operation.monthly.entity.AnnualReportBackupFile;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingReportFile;
import com.project.backend.features.operation.monthly.enums.AnnualReportBackupStatus;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.repository.AnnualReportBackupExecutionRepository;
import com.project.backend.features.operation.monthly.repository.AnnualReportBackupFileRepository;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingOutputDefinitionRepository;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingReportFileRepository;
import com.project.backend.features.system.report.repository.ReportHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnnualReportBackupService {

    private static final int MAX_ERROR_LENGTH = 4000;

    private final AnnualReportBackupExecutionRepository executionRepository;
    private final AnnualReportBackupFileRepository backupFileRepository;
    private final MonthlyClosingOutputDefinitionRepository definitionRepository;
    private final MonthlyClosingReportFileRepository reportFileRepository;
    private final ReportHistoryRepository reportHistoryRepository;
    private final StorageService storageService;
    private final AnnualReportBackupKeyBuilder keyBuilder;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Integer> findPendingFiscalYears(
            int fiscalYearStartMonth,
            int graceDays
    ) {
        validate(2000, fiscalYearStartMonth, graceDays);
        String tenantId = requireTenantId();
        Map<String, Integer> retentionYears = retentionYears(tenantId);
        LocalDate today = LocalDate.now(clock);
        SortedSet<Integer> fiscalYears = new TreeSet<>();

        for (MonthlyClosingReportFile source : reportFileRepository
                .findByTenantIdAndDeletedAtIsNullOrderByTargetMonthAscIdAsc(
                        tenantId
                )) {
            if (resolveRetentionYears(
                    retentionYears,
                    source.getReportCode()
            ) == null) {
                continue;
            }
            YearMonth targetMonth = YearMonth.parse(source.getTargetMonth());
            int fiscalYear = targetMonth.getMonthValue()
                    >= fiscalYearStartMonth
                    ? targetMonth.getYear()
                    : targetMonth.getYear() - 1;
            LocalDate periodEnd = LocalDate.of(
                    fiscalYear,
                    fiscalYearStartMonth,
                    1
            ).plusYears(1).minusDays(1);
            if (today.isBefore(periodEnd.plusDays(graceDays))) {
                continue;
            }
            boolean completed = executionRepository
                    .findByTenantIdAndFiscalYearAndDeletedAtIsNull(
                            tenantId,
                            fiscalYear
                    )
                    .map(execution -> execution.getStatus()
                            == AnnualReportBackupStatus.COMPLETED)
                    .orElse(false);
            if (!completed) {
                fiscalYears.add(fiscalYear);
            }
        }
        return List.copyOf(fiscalYears);
    }

    @Transactional
    public AnnualReportBackupResult execute(
            int fiscalYear,
            int fiscalYearStartMonth,
            int graceDays
    ) {
        validate(fiscalYear, fiscalYearStartMonth, graceDays);
        String tenantId = requireTenantId();
        LocalDate periodStart = LocalDate.of(
                fiscalYear,
                fiscalYearStartMonth,
                1
        );
        LocalDate periodEnd = periodStart.plusYears(1).minusDays(1);
        LocalDate eligibleDate = periodEnd.plusDays(graceDays);
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(eligibleDate)) {
            throw new IllegalStateException(
                    "年度帳票バックアップの実行可能日前です。eligibleDate="
                            + eligibleDate
            );
        }

        AnnualReportBackupExecution execution = executionRepository
                .findByTenantIdAndFiscalYearAndDeletedAtIsNull(
                        tenantId,
                        fiscalYear
                )
                .orElseGet(AnnualReportBackupExecution::new);
        if (execution.getStatus() == AnnualReportBackupStatus.COMPLETED) {
            return toResult(execution);
        }

        Instant now = Instant.now(clock);
        initializeExecution(
                execution,
                fiscalYear,
                periodStart,
                periodEnd,
                eligibleDate,
                now
        );
        execution = executionRepository.saveAndFlush(execution);

        try {
            Map<String, Integer> retentionYears = retentionYears(tenantId);
            List<MonthlyClosingReportFile> sources = reportFileRepository
                    .findByTenantIdAndTargetMonthBetweenAndDeletedAtIsNullOrderByTargetMonthAscIdAsc(
                            tenantId,
                            YearMonth.from(periodStart).toString(),
                            YearMonth.from(periodEnd).toString()
                    )
                    .stream()
                    .filter(source -> resolveRetentionYears(
                            retentionYears,
                            source.getReportCode()
                    ) != null)
                    .toList();

            for (MonthlyClosingReportFile source : sources) {
                copyIfNecessary(
                        execution,
                        source,
                        tenantId,
                        fiscalYear,
                        periodEnd,
                        resolveRetentionYears(
                                retentionYears,
                                source.getReportCode()
                        ),
                        now
                );
            }

            List<AnnualReportBackupFile> backedUpFiles =
                    backupFileRepository
                            .findByTenantIdAndBackupExecutionIdAndDeletedAtIsNullOrderByIdAsc(
                                    tenantId,
                                    execution.getId()
                            );
            if (backedUpFiles.size() != sources.size()) {
                throw new IllegalStateException(
                        "年度帳票バックアップ件数が一致しません。source="
                                + sources.size()
                                + ", backup="
                                + backedUpFiles.size()
                );
            }

            List<String> sourceKeys = sources.stream()
                    .map(MonthlyClosingReportFile::getOutputFileKey)
                    .toList();
            if (!sourceKeys.isEmpty()) {
                reportHistoryRepository.softDeleteByStoredFileKeys(
                        tenantId,
                        sourceKeys,
                        now
                );
                sources.forEach(source -> source.setDeletedAt(now));
                reportFileRepository.saveAll(sources);
            }

            execution.setStatus(AnnualReportBackupStatus.COMPLETED);
            execution.setCompletedAt(now);
            execution.setFileCount(backedUpFiles.size());
            execution.setTotalSize(backedUpFiles.stream()
                    .mapToLong(AnnualReportBackupFile::getFileSize)
                    .sum());
            execution.setErrorMessage(null);
            executionRepository.save(execution);
            return toResult(execution);
        } catch (Exception exception) {
            execution.setStatus(AnnualReportBackupStatus.FAILED);
            execution.setCompletedAt(now);
            execution.setErrorMessage(limitError(exception));
            executionRepository.save(execution);
            return toResult(execution);
        }
    }

    private void copyIfNecessary(
            AnnualReportBackupExecution execution,
            MonthlyClosingReportFile source,
            String tenantId,
            int fiscalYear,
            LocalDate periodEnd,
            int retentionYears,
            Instant now
    ) {
        if (backupFileRepository
                .existsByTenantIdAndMonthlyClosingReportFileIdAndDeletedAtIsNull(
                        tenantId,
                        source.getId()
                )) {
            return;
        }
        validateSource(source);
        if (!storageService.exists(
                source.getStorageType(),
                source.getOutputFileKey()
        )) {
            throw new IllegalStateException(
                    "バックアップ元帳票が存在しません。fileKey="
                            + source.getOutputFileKey()
            );
        }

        String backupKey = keyBuilder.build(
                tenantId,
                fiscalYear,
                source
        );
        storageService.copy(
                source.getStorageType(),
                source.getOutputFileKey(),
                backupKey
        );
        if (!storageService.exists(source.getStorageType(), backupKey)) {
            throw new IllegalStateException(
                    "バックアップ先帳票を確認できません。fileKey="
                            + backupKey
            );
        }

        AnnualReportBackupFile backup = new AnnualReportBackupFile();
        backup.setBackupExecutionId(execution.getId());
        backup.setMonthlyClosingReportFileId(source.getId());
        backup.setReportCode(source.getReportCode());
        backup.setTargetMonth(source.getTargetMonth());
        backup.setClosingVersion(source.getClosingVersion());
        backup.setStorageType(source.getStorageType());
        backup.setSourceFileKey(source.getOutputFileKey());
        backup.setBackupFileKey(backupKey);
        backup.setFileName(source.getOutputFileName());
        backup.setFileSize(source.getFileSize());
        backup.setCopiedAt(now);
        backup.setRetentionUntil(periodEnd.plusYears(retentionYears));
        backupFileRepository.save(backup);
    }

    private Map<String, Integer> retentionYears(String tenantId) {
        return definitionRepository
                .findByTenantIdAndOutputTypeAndActiveFlagTrueAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc(
                        tenantId,
                        MonthlyClosingOutputType.REPORT
                )
                .stream()
                .filter(definition -> definition.getBackupRetentionYears() != null)
                .filter(definition -> definition.getBackupRetentionYears() > 0)
                .collect(Collectors.toMap(
                        MonthlyClosingOutputDefinition::getOutputCode,
                        MonthlyClosingOutputDefinition::getBackupRetentionYears,
                        (first, second) -> first
                ));
    }

    private Integer resolveRetentionYears(
            Map<String, Integer> retentionYears,
            String reportCode
    ) {
        Integer direct = retentionYears.get(reportCode);
        if (direct != null) {
            return direct;
        }
        if (StringUtils.hasText(reportCode)
                && reportCode.startsWith("MONTHLY_INVOICE_PATTERN_")) {
            return retentionYears.get("MONTHLY_INVOICE");
        }
        return null;
    }

    private void initializeExecution(
            AnnualReportBackupExecution execution,
            int fiscalYear,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate eligibleDate,
            Instant now
    ) {
        execution.setFiscalYear(fiscalYear);
        execution.setPeriodStart(periodStart);
        execution.setPeriodEnd(periodEnd);
        execution.setEligibleDate(eligibleDate);
        execution.setStatus(AnnualReportBackupStatus.PROCESSING);
        execution.setStartedAt(now);
        execution.setCompletedAt(null);
        execution.setFileCount(0);
        execution.setTotalSize(0L);
        execution.setErrorMessage(null);
    }

    private void validateSource(MonthlyClosingReportFile source) {
        if (source.getStorageType() == null
                || !StringUtils.hasText(source.getOutputFileKey())
                || !StringUtils.hasText(source.getOutputFileName())
                || source.getFileSize() == null
                || source.getFileSize() < 0) {
            throw new IllegalStateException(
                    "バックアップ元帳票の保存情報が不完全です。id="
                            + source.getId()
            );
        }
    }

    private void validate(
            int fiscalYear,
            int fiscalYearStartMonth,
            int graceDays
    ) {
        if (fiscalYear < 2000 || fiscalYear > 9999) {
            throw new IllegalArgumentException("会計年度が不正です。");
        }
        if (fiscalYearStartMonth < 1 || fiscalYearStartMonth > 12) {
            throw new IllegalArgumentException("会計年度開始月は1～12で指定してください。");
        }
        if (graceDays < 0 || graceDays > 90) {
            throw new IllegalArgumentException("バックアップ猶予日数は0～90で指定してください。");
        }
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("テナント情報を取得できません。");
        }
        return tenantId;
    }

    private String limitError(Exception exception) {
        String message = StringUtils.hasText(exception.getMessage())
                ? exception.getMessage()
                : "年度帳票バックアップに失敗しました。";
        return message.length() <= MAX_ERROR_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_LENGTH);
    }

    private AnnualReportBackupResult toResult(
            AnnualReportBackupExecution execution
    ) {
        return new AnnualReportBackupResult(
                execution.getId(),
                execution.getFiscalYear(),
                execution.getStatus(),
                execution.getFileCount(),
                execution.getTotalSize(),
                execution.getErrorMessage()
        );
    }
}
