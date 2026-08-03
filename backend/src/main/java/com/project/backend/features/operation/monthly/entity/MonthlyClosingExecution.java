package com.project.backend.features.operation.monthly.entity;

import java.time.Instant;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingExecutionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "monthly_closing_execution",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_monthly_closing_execution_version",
                columnNames = {
                        "tenant_id",
                        "monthly_closing_id",
                        "closing_version"
                }
        ),
        indexes = @Index(
                name = "idx_monthly_closing_execution_status",
                columnList = "tenant_id,status,started_at"
        )
)
@Getter
@Setter
public class MonthlyClosingExecution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monthly_closing_id", nullable = false)
    private Long monthlyClosingId;

    @Column(name = "closing_version", nullable = false)
    private Integer closingVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MonthlyClosingExecutionStatus status =
            MonthlyClosingExecutionStatus.PROCESSING;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "executed_by", nullable = false, length = 100)
    private String executedBy;

    @Lob
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;
}
