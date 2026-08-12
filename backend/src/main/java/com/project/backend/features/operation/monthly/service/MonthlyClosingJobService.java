package com.project.backend.features.operation.monthly.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingOutputDefinitionRepository;
import com.project.backend.features.operation.monthly.service.executor.MonthlyClosingJobExecutor;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MonthlyClosingJobService {

    private static final String MONTHLY_INVOICE_JOB_CODE =
            "PRINT_MONTHLY_INVOICE";
    private static final String MONTHLY_ORDER_FORM_JOB_CODE =
            "PRINT_MONTHLY_ORDER_FORM";

    private final OperationReportPreviewRepository previewRepository;
    private final MonthlyClosingOutputDefinitionRepository outputDefinitionRepository;
    private final MonthlyClosingJobExecutor executor;

    public void executeClosing(
            Long monthlyClosingId,
            MonthlyClosingPeriod period,
            Integer closingVersion
    ) {
        List<OperationReportPreview> previews = resolveClosingReports();
        if (previews.isEmpty()) {
            throw new IllegalStateException(
                    "有効な月次締め帳票が設定されていません。"
            );
        }

        for (OperationReportPreview preview : previews) {
            String jobCode =
                    preview.getJobCode();

            if (jobCode == null || jobCode.isBlank()) {
                throw new IllegalStateException(
                        "月次締め帳票のjobCodeが未設定です。reportCode="
                                + preview.getReportCode()
                );
            }

            if (MONTHLY_INVOICE_JOB_CODE.equals(jobCode)
                    || MONTHLY_ORDER_FORM_JOB_CODE.equals(jobCode)) {
                // 顧客向け帳票は顧客別締日の「顧客請求締め」で確定する。
                continue;
            }

            executor.execute(
                    monthlyClosingId,
                    preview,
                    period,
                    closingVersion
            );
        }

    }

    private List<OperationReportPreview> resolveClosingReports() {
        List<MonthlyClosingOutputDefinition> definitions =
                outputDefinitionRepository
                        .findByOutputTypeAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc(
                                MonthlyClosingOutputType.REPORT
                        );

        if (definitions.isEmpty()) {
            return previewRepository
                    .findByOperationTypeAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                            OperationType.MONTHLY
                    );
        }

        return definitions.stream()
                .filter(definition -> Boolean.TRUE.equals(definition.getActiveFlag()))
                .map(definition -> {
                    OperationReportPreview preview = previewRepository
                            .findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                                    OperationType.MONTHLY,
                                    definition.getOutputCode()
                            )
                            .orElseThrow(() -> new IllegalStateException(
                                    "締め帳票に対応する月次帳票が見つかりません。reportCode="
                                            + definition.getOutputCode()
                            ));
                    if (!Boolean.TRUE.equals(preview.getActiveFlag())) {
                        throw new IllegalStateException(
                                "締め帳票が帳票管理側で無効です。reportCode="
                                        + definition.getOutputCode()
                        );
                    }
                    return preview;
                })
                .toList();
    }

}
