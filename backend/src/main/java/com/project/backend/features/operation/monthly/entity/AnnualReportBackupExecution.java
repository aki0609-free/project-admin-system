package com.project.backend.features.operation.monthly.entity;

import java.time.Instant;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.operation.monthly.enums.AnnualReportBackupStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "annual_report_backup_execution",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_annual_report_backup_fiscal_year",
                columnNames = {"tenant_id", "fiscal_year"}
        )
)
@Getter
@Setter
public class AnnualReportBackupExecution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "eligible_date", nullable = false)
    private LocalDate eligibleDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AnnualReportBackupStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "file_count", nullable = false)
    private Integer fileCount = 0;

    @Column(name = "total_size", nullable = false)
    private Long totalSize = 0L;

    @Lob
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;
}
