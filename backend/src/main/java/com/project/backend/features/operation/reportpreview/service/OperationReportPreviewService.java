package com.project.backend.features.operation.reportpreview.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.operation.reportpreview.dto.OperationReportPreviewResponse;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreviewColumn;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.operation.reportpreview.mapper.OperationReportPreviewMapper;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewColumnRepository;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewRepository;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.repository.ReportMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationReportPreviewService {

    private final OperationReportPreviewRepository previewRepository;
    private final OperationReportPreviewColumnRepository columnRepository;
    private final ReportMasterRepository reportMasterRepository;
    private final OperationReportPreviewMapper mapper;

    public List<OperationReportPreviewResponse> findReports(OperationType operationType) {
        return previewRepository
                .findByOperationTypeAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(operationType)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OperationReportPreview findDefinition(
            OperationType operationType,
            String reportCode) {

        return previewRepository
                .findByOperationTypeAndReportCodeAndActiveFlagTrueAndDeletedAtIsNull(
                        operationType,
                        reportCode)
                .orElseThrow(() -> new RuntimeException("プレビュー定義が見つかりません。"));
    }

    private OperationReportPreviewResponse toResponse(OperationReportPreview preview) {
        List<OperationReportPreviewColumn> columns =
                columnRepository
                        .findByPreviewIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                                preview.getId());

        ReportMaster reportMaster =
                reportMasterRepository
                        .findByReportCodeAndActiveFlagTrueAndDeletedAtIsNull(preview.getReportCode())
                        .orElse(null);

        return mapper.toResponse(preview, reportMaster, columns);
    }
}