package com.project.backend.features.operation.monthly.entity;

import java.time.Instant;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingItemStatus;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;

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
        name = "monthly_closing_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_monthly_closing_item_target",
                columnNames = {
                        "tenant_id",
                        "monthly_closing_execution_id",
                        "output_type",
                        "output_code",
                        "target_key"
                }
        ),
        indexes = @Index(
                name = "idx_monthly_closing_item_status",
                columnList =
                        "tenant_id,monthly_closing_execution_id,status"
        )
)
@Getter
@Setter
public class MonthlyClosingItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monthly_closing_execution_id", nullable = false)
    private Long monthlyClosingExecutionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_type", nullable = false, length = 30)
    private MonthlyClosingOutputType outputType;

    @Column(name = "output_code", nullable = false, length = 100)
    private String outputCode;

    @Column(name = "target_key", nullable = false, length = 255)
    private String targetKey = "ALL";

    @Column(name = "required_flag", nullable = false)
    private Boolean requiredFlag = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MonthlyClosingItemStatus status =
            MonthlyClosingItemStatus.WAITING;

    @Column(name = "history_row_count")
    private Long historyRowCount;

    @Column(name = "history_table", length = 200)
    private String historyTable;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", length = 30)
    private StorageType storageType;

    @Column(name = "file_key", length = 1000)
    private String fileKey;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_hash", length = 128)
    private String fileHash;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Lob
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;
}
