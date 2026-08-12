package com.project.backend.features.operation.monthly.service.executor;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingReportTarget;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingReportFile;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingReportFileRepository;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.dto.BatchJobRunResult;
import com.project.backend.features.system.batch.service.BatchExecutionService;
import com.project.backend.features.system.batch.service.executor.ReportBatchJobExecutor;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonthlyClosingJobExecutor {

        private final BatchExecutionService batchExecutionService;
        private final MonthlyClosingReportFileRepository reportFileRepository;
        private final Clock clock;

        public void execute(
                        Long monthlyClosingId,
                        OperationReportPreview preview,
                        MonthlyClosingPeriod period,
                        Integer closingVersion,
                        MonthlyClosingReportTarget target) {
                execute(
                                monthlyClosingId,
                                preview,
                                period,
                                closingVersion,
                                target,
                                null);
        }

        public void execute(
                        Long monthlyClosingId,
                        OperationReportPreview preview,
                        MonthlyClosingPeriod period,
                        Integer closingVersion,
                        MonthlyClosingReportTarget target,
                        String resolvedReportCode) {
                execute(
                                monthlyClosingId,
                                preview,
                                period,
                                closingVersion,
                                target,
                                resolvedReportCode,
                                "COMPANY");
        }

        public void execute(
                        Long closingReferenceId,
                        OperationReportPreview preview,
                        MonthlyClosingPeriod period,
                        Integer closingVersion,
                        MonthlyClosingReportTarget target,
                        String resolvedReportCode,
                        String closingScope) {
                Map<String, Object> parameters = new HashMap<>();

                parameters.put("targetMonth", period.targetMonth());
                parameters.put("closingStartDate", period.startDate().toString());
                parameters.put("closingEndDate", period.endDate().toString());
                parameters.put("periodFrom", period.startDate().toString());
                parameters.put("periodTo", period.endDate().toString());
                parameters.put("closingVersion", closingVersion);
                parameters.put(
                                "executionMode",
                                closingVersion != null && closingVersion > 1
                                                ? "RECLOSE"
                                                : "INITIAL");

                if (target != null) {
                        parameters.put("targetType", target.targetType());

                        if (target.targetId() != null) {
                                parameters.put("targetId", target.targetId());
                        }

                        /*
                         * 月次請求書側の既存処理と合わせるため、
                         * CUSTOMERの場合のみcustomerIdも渡す。
                         */
                        if ("CUSTOMER".equals(target.targetType()) && target.targetId() != null) {
                                parameters.put("customerId", target.targetId());
                        }
                }

                if (resolvedReportCode != null
                                && !resolvedReportCode.isBlank()) {
                        parameters.put(
                                        ReportBatchJobExecutor.RESOLVED_REPORT_CODE_PARAM,
                                        resolvedReportCode);
                }

                BatchJobRunResult runResult = batchExecutionService.executeNowForResult(
                                preview.getJobCode(),
                                parameters);

                BatchJobExecutionResult result = runResult.result();
                validateGeneratedFile(preview, result);
                MonthlyClosingReportFile file = new MonthlyClosingReportFile();

                file.setMonthlyClosingId(closingReferenceId);
                file.setClosingScope(closingScope);
                file.setTargetMonth(period.targetMonth());
                file.setClosingVersion(closingVersion);
                file.setReportCode(
                                resolvedReportCode != null
                                                && !resolvedReportCode.isBlank()
                                                                ? resolvedReportCode
                                                                : preview.getReportCode());
                file.setJobCode(preview.getJobCode());

                if (target != null) {
                        file.setTargetType(target.targetType());
                        file.setTargetId(target.targetId());
                        file.setTargetName(target.targetName());
                } else {
                        file.setTargetType("ALL");
                        file.setTargetName("全体");
                }

                file.setBatchExecutionLogId(runResult.executionLogId());

                file.setGeneratedAt(Instant.now(clock));

                if (result != null) {
                        file.setStorageType(result.storageType());
                        file.setOutputFileKey(result.outputFileKey());
                        file.setOutputFileName(result.outputFileName());
                        file.setContentType(result.contentType());
                        file.setFileSize(result.fileSize());
                }

                reportFileRepository.save(file);
        }

        private void validateGeneratedFile(
                        OperationReportPreview preview,
                        BatchJobExecutionResult result) {
                if (!requiresGeneratedFile(preview.getOutputType())) {
                        return;
                }
                if (result == null
                                || result.storageType() == null
                                || !StringUtils.hasText(result.outputFileKey())
                                || !StringUtils.hasText(result.outputFileName())
                                || result.fileSize() == null
                                || result.fileSize() <= 0) {
                        throw new IllegalStateException(
                                        "月次締め帳票ファイルが生成されませんでした。reportCode="
                                                        + preview.getReportCode());
                }
        }

        private boolean requiresGeneratedFile(
                        OperationReportOutputType outputType) {
                return outputType == OperationReportOutputType.PDF
                                || outputType == OperationReportOutputType.CSV
                                || outputType == OperationReportOutputType.EXCEL;
        }

        public void execute(
                        Long monthlyClosingId,
                        OperationReportPreview preview,
                        MonthlyClosingPeriod period,
                        Integer closingVersion) {
                execute(
                                monthlyClosingId,
                                preview,
                                period,
                                closingVersion,
                                MonthlyClosingReportTarget.all());
        }
}
