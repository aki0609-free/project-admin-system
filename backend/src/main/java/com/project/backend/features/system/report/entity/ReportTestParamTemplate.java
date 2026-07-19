package com.project.backend.features.system.report.entity;

import com.project.backend.app.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "report_test_param_template",
        indexes = {
                @Index(name = "idx_report_test_param_report_code", columnList = "report_code")
        }
)
@Getter
@Setter
public class ReportTestParamTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_code", nullable = false, length = 100)
    private String reportCode;

    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;

    @Lob
    @Column(name = "json_text", nullable = false, columnDefinition = "LONGTEXT")
    private String jsonText;

    @Column(name = "default_flag", nullable = false)
    private Boolean defaultFlag = false;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = true;
}