package com.project.backend.features.operation.monthly.entity;

import java.time.Instant;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.app.storage.enums.StorageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "annual_report_backup_file",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_annual_report_backup_source_file",
                columnNames = {
                        "tenant_id",
                        "monthly_closing_report_file_id"
                }
        )
)
@Getter
@Setter
public class AnnualReportBackupFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "backup_execution_id", nullable = false)
    private Long backupExecutionId;

    @Column(name = "monthly_closing_report_file_id", nullable = false)
    private Long monthlyClosingReportFileId;

    @Column(name = "report_code", nullable = false, length = 100)
    private String reportCode;

    @Column(name = "target_month", nullable = false, length = 7)
    private String targetMonth;

    @Column(name = "closing_version", nullable = false)
    private Integer closingVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 30)
    private StorageType storageType;

    @Column(name = "source_file_key", nullable = false, length = 1000)
    private String sourceFileKey;

    @Column(name = "backup_file_key", nullable = false, length = 1000)
    private String backupFileKey;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "copied_at", nullable = false)
    private Instant copiedAt;

    @Column(name = "retention_until", nullable = false)
    private LocalDate retentionUntil;
}
