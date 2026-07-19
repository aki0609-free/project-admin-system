package com.project.backend.features.system.report.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.report.enums.ReportParamControlType;
import com.project.backend.features.system.report.enums.ReportParamType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "report_param")
@Getter
@Setter
public class ReportParam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_master_id", nullable = false)
    private ReportMaster reportMaster;

    @Column(name = "param_name", nullable = false, length = 100)
    private String paramName;

    @Column(name = "param_label", nullable = false, length = 200)
    private String paramLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "param_type", nullable = false, length = 30)
    private ReportParamType paramType;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_type", nullable = false, length = 30)
    private ReportParamControlType controlType = ReportParamControlType.TEXT;

    @Column(name = "required_flag", nullable = false)
    private Boolean requiredFlag = false;

    @Column(name = "visible_flag", nullable = false)
    private Boolean visibleFlag = true;

    @Column(name = "multiple_flag", nullable = false)
    private Boolean multipleFlag = false;

    @Column(name = "filter_flag", nullable = false)
    private Boolean filterFlag = true;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "placeholder", length = 255)
    private String placeholder;

    @Column(name = "input_column_name", length = 100)
    private String inputColumnName;

    @Column(name = "display_order")
    private Integer displayOrder = 1;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = true;

    public String resolveInputColumnName() {
        if (inputColumnName != null && !inputColumnName.isBlank()) {
            return inputColumnName;
        }
        return paramName;
    }
}