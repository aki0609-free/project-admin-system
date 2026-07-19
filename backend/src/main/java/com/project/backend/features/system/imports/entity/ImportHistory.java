package com.project.backend.features.system.imports.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.imports.enums.ImportHistoryStatus;
import com.project.backend.features.system.imports.enums.ImportMode;
import com.project.backend.features.system.imports.enums.ImportSourceType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "import_history",
        indexes = {
                @Index(name = "idx_import_history_target_code", columnList = "target_code"),
                @Index(name = "idx_import_history_status", columnList = "status"),
                @Index(name = "idx_import_history_executed_at", columnList = "executed_at")
        }
)
@Getter
@Setter
public class ImportHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_code", nullable = false, length = 100)
    private String targetCode;

    @Column(name = "target_name", length = 200)
    private String targetName;

    @Column(name = "table_name", length = 200)
    private String tableName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30)
    private ImportSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_mode", length = 30)
    private ImportMode importMode;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "inserted_count", nullable = false)
    private int insertedCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ImportHistoryStatus status;

    @Column(name = "job_execution_id")
    private Long jobExecutionId;

    @Column(name = "executed_by", length = 100)
    private String executedBy;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @OneToMany(mappedBy = "history", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportErrorRow> errorRows = new ArrayList<>();

    public void addErrorRow(ImportErrorRow errorRow) {
        errorRow.setHistory(this);
        this.errorRows.add(errorRow);
    }
}