package com.project.backend.features.system.report.entity;

import java.util.ArrayList;
import java.util.List;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.report.enums.ReportCleanupType;
import com.project.backend.features.system.report.enums.ReportLayoutType;
import com.project.backend.features.system.report.enums.ReportOutputFormat;
import com.project.backend.features.system.report.enums.ReportPreProcessType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "report_master")
@Getter
@Setter
public class ReportMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_code", nullable = false, unique = true, length = 100)
    private String reportCode;

    @Column(name = "report_name", nullable = false, length = 200)
    private String reportName;

    @Column(name = "template_file_name", length = 255)
    private String templateFileName;

    /**
     * 基本名
     * 例: salary_slip
     */
    @Column(name = "work_table", nullable = false, length = 200)
    private String workTable;

    /**
     * override用
     * nullなら work_table + "_input"
     */
    @Column(name = "input_table", length = 200)
    private String inputTable;

    /**
     * override用
     * nullなら work_table + "_output"
     */
    @Column(name = "output_table", length = 200)
    private String outputTable;

    @Enumerated(EnumType.STRING)
    @Column(name = "pre_process_type", nullable = false, length = 30)
    private ReportPreProcessType preProcessType = ReportPreProcessType.NONE;

    @Lob
    @Column(
        name = "pre_process_sql",
        columnDefinition = "LONGTEXT"
    )
    private String preProcessSql;

    @Column(name = "procedure_name", length = 255)
    private String procedureName;

    @Lob
    @Column(name = "query_sql")
    private String querySql;

    @Enumerated(EnumType.STRING)
    @Column(name = "cleanup_type", nullable = false, length = 30)
    private ReportCleanupType cleanupType = ReportCleanupType.NONE;

    @Lob
    @Column(name = "cleanup_sql")
    private String cleanupSql;

    @Column(name = "cleanup_procedure_name", length = 255)
    private String cleanupProcedureName;

    @Enumerated(EnumType.STRING)
    @Column(name = "layout_type", length = 50)
    private ReportLayoutType layoutType = ReportLayoutType.SINGLE;

    @Column(name = "layout_count")
    private Integer layoutCount = 1;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", nullable = false, length = 20)
    private ReportOutputFormat outputFormat = ReportOutputFormat.PDF;

    @Column(name = "use_signature", nullable = false)
    private Boolean useSignature = false;

    @Column(name = "preview_enabled", nullable = false)
    private Boolean previewEnabled = true;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = true;

    @OneToMany(mappedBy = "reportMaster", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReportParam> params = new ArrayList<>();

    @OneToMany(mappedBy = "reportMaster", fetch = FetchType.LAZY)
    private List<ReportHistory> histories = new ArrayList<>();

    @OneToMany(mappedBy = "reportMaster", fetch = FetchType.LAZY)
    private List<ReportSignature> signatures = new ArrayList<>();

    public void addParam(ReportParam param) {
        param.setReportMaster(this);
        this.params.add(param);
    }

    public void clearParams() {
        for (ReportParam param : this.params) {
            param.setReportMaster(null);
        }
        this.params.clear();
    }

    public String resolveInputTableName() {
        if (inputTable != null && !inputTable.isBlank()) {
            return inputTable;
        }
        return workTable + "_input";
    }

    public String resolveOutputTableName() {
        if (outputTable != null && !outputTable.isBlank()) {
            return outputTable;
        }
        return workTable + "_output";
    }
}