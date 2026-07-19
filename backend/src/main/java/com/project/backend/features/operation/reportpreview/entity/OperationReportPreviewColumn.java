package com.project.backend.features.operation.reportpreview.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "operation_report_preview_column")
@Getter
@Setter
public class OperationReportPreviewColumn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_report_preview_id", nullable = false)
    private OperationReportPreview preview;

    @Column(name = "preview_name", nullable = false, length = 100)
    private String previewName;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "display_order")
    private Integer displayOrder = 1;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = true;
}