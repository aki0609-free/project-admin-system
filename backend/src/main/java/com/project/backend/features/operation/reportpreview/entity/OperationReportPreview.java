package com.project.backend.features.operation.reportpreview.entity;

import java.util.ArrayList;
import java.util.List;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;
import com.project.backend.features.operation.reportpreview.enums.OperationType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "operation_report_preview", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "tenant_id", "operation_type", "report_code" })
})
@Getter
@Setter
public class OperationReportPreview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    private OperationType operationType;

    @Column(name = "report_code", nullable = false, length = 100)
    private String reportCode;

    @Column(name = "job_code", length = 100)
    private String jobCode;

    @Column(name = "table_name", nullable = false, length = 200)
    private String tableName;

    @Column(name = "template_name", nullable = false, length = 255)
    private String templateName;

    @Column(name = "order_by", length = 500)
    private String orderBy;

    @Column(name = "display_order")
    private Integer displayOrder = 1;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_type", nullable = false, length = 30)
    private OperationReportOutputType outputType = OperationReportOutputType.PDF;

    @OneToMany(mappedBy = "preview", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, id ASC")
    private List<OperationReportPreviewColumn> columns = new ArrayList<>();

    public void addColumn(OperationReportPreviewColumn column) {
        column.setPreview(this);
        this.columns.add(column);
    }

    public void clearColumns() {
        for (OperationReportPreviewColumn column : this.columns) {
            column.setPreview(null);
        }
        this.columns.clear();
    }
}