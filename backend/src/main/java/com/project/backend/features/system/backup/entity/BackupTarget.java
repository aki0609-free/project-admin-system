package com.project.backend.features.system.backup.entity;

import java.util.ArrayList;
import java.util.List;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.backup.enums.BackupOutputMode;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "backup_target")
@Getter
@Setter
public class BackupTarget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_code", nullable = false, unique = true, length = 100)
    private String targetCode;

    @Column(name = "target_name", nullable = false, length = 200)
    private String targetName;

    @Column(name = "table_name", nullable = false, length = 200)
    private String tableName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "backup_enabled", nullable = false)
    private boolean backupEnabled = true;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_mode", nullable = false, length = 30)
    private BackupOutputMode outputMode = BackupOutputMode.DOWNLOAD;

    @Column(name = "output_dir", length = 500)
    private String outputDir;

    @Column(name = "file_name_pattern", length = 200)
    private String fileNamePattern;

    @Column(name = "zip_required", nullable = false)
    private boolean zipRequired = false;

    @Column(name = "include_header", nullable = false)
    private boolean includeHeader = true;

    @OneToMany(mappedBy = "target", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<BackupColumn> columns = new ArrayList<>();

    public void addColumn(BackupColumn column) {
        column.setTarget(this);
        this.columns.add(column);
    }

    public void clearColumns() {
        this.columns.clear();
    }
}