package com.project.backend.features.operation.reportpreview.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.reportpreview.entity.OperationReportPreviewColumn;

public interface OperationReportPreviewColumnRepository
        extends JpaRepository<OperationReportPreviewColumn, Long> {

    List<OperationReportPreviewColumn>
            findByPreviewIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                    Long previewId);
}