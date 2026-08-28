package com.project.backend.features.operation.monthly.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.service.executor.MonthlyClosingJobExecutor;
import com.project.backend.features.operation.book.service.SpreadsheetLedgerGenerationService;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MonthlyClosingJobService {

    private final OperationReportPreviewRepository previewRepository;
    private final MonthlyClosingJobExecutor executor;
    private final SpreadsheetLedgerGenerationService ledgerGenerationService;
    private final MonthlyClosingOutputDefinitionService outputDefinitionService;

    public void executeClosing(
            Long monthlyClosingId,
            MonthlyClosingPeriod period,
            Integer closingVersion
    ) {
        executeClosing(
                monthlyClosingId,
                period,
                closingVersion,
                outputDefinitionService.findActiveCompanyOutputs()
        );
    }

    public void executeClosing(
            Long monthlyClosingId,
            MonthlyClosingPeriod period,
            Integer closingVersion,
            List<MonthlyClosingOutputDefinition> definitions
    ) {
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalStateException(
                    "有効な月次締め帳票・台帳が設定されていません。"
            );
        }

        for (MonthlyClosingOutputDefinition definition
                : definitions) {
            executeDefinition(
                    monthlyClosingId,
                    period,
                    closingVersion,
                    definition
            );
        }
    }

    private void executeDefinition(
            Long monthlyClosingId,
            MonthlyClosingPeriod period,
            Integer closingVersion,
            MonthlyClosingOutputDefinition definition
    ) {
        if (definition.getOutputType() == MonthlyClosingOutputType.LEDGER) {
            ledgerGenerationService.generateForClosing(
                    definition.getOutputCode(),
                    period.targetMonth(),
                    closingVersion
            );
            return;
        }

        if (definition.getOutputType() != MonthlyClosingOutputType.REPORT) {
            throw new IllegalStateException(
                    "未対応の月次締め出力区分です。outputType="
                            + definition.getOutputType()
            );
        }

        OperationReportPreview preview = previewRepository
                .findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                        OperationType.MONTHLY,
                        definition.getOutputCode()
                )
                .filter(item -> Boolean.TRUE.equals(item.getActiveFlag()))
                .orElseThrow(() -> new IllegalStateException(
                        "有効な締め帳票が見つかりません。reportCode="
                                + definition.getOutputCode()
                ));
        if (preview.getJobCode() == null || preview.getJobCode().isBlank()) {
            throw new IllegalStateException(
                    "月次締め帳票のjobCodeが未設定です。reportCode="
                            + preview.getReportCode()
            );
        }
        executor.execute(
                monthlyClosingId,
                preview,
                period,
                closingVersion
        );
    }

}
