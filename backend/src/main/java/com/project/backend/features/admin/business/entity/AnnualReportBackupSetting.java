package com.project.backend.features.admin.business.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "annual_report_backup_setting",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_annual_report_backup_setting_code",
                columnNames = {"tenant_id", "setting_code"}
        )
)
@Getter
@Setter
public class AnnualReportBackupSetting extends BaseEntity {

    public static final String DEFAULT_SETTING_CODE = "DEFAULT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_code", nullable = false, length = 50)
    private String settingCode = DEFAULT_SETTING_CODE;

    @Column(name = "fiscal_year_start_month", nullable = false)
    private Integer fiscalYearStartMonth = 4;

    @Column(name = "grace_days", nullable = false)
    private Integer graceDays = 14;

    @Column(name = "startup_enabled", nullable = false)
    private Boolean startupEnabled = true;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = true;
}
