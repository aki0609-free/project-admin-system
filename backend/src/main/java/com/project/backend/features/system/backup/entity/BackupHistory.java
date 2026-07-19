package com.project.backend.features.system.backup.entity;

import java.time.Instant;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.system.backup.enums.BackupHistoryStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "backup_history",
        indexes = {
                @Index(name = "idx_backup_history_status", columnList = "status"),
                @Index(name = "idx_backup_history_executed_at", columnList = "executed_at"),
                @Index(name = "idx_backup_history_storage_type", columnList = "storage_type")
        }
)
@Getter
@Setter
public class BackupHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_codes", nullable = false, length = 2000)
    private String targetCodes;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "zip_output", nullable = false)
    private boolean zipOutput = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", length = 30)
    private StorageType storageType;

    @Column(name = "stored_file_key", length = 1000)
    private String storedFileKey;

    @Column(name = "stored_file_name", length = 500)
    private String storedFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BackupHistoryStatus status;

    @Column(name = "executed_by", length = 100)
    private String executedBy;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;
}