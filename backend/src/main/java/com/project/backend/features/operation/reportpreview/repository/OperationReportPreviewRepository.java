package com.project.backend.features.operation.reportpreview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationType;

public interface OperationReportPreviewRepository
        extends JpaRepository<OperationReportPreview, Long> {

    List<OperationReportPreview>
            findByOperationTypeAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                    OperationType operationType);

    List<OperationReportPreview>
            findByOperationTypeAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                    OperationType operationType);

    Optional<OperationReportPreview>
            findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                    OperationType operationType,
                    String reportCode);

    Optional<OperationReportPreview>
            findByOperationTypeAndReportCodeAndActiveFlagTrueAndDeletedAtIsNull(
                    OperationType operationType,
                    String reportCode);
}
