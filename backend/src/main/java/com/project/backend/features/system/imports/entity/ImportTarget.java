package com.project.backend.features.system.imports.entity;

import java.util.ArrayList;
import java.util.List;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.imports.enums.ImportMode;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.enums.ImportSourceType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "import_target")
@Getter
@Setter
public class ImportTarget extends BaseEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ImportSourceType sourceType = ImportSourceType.UPLOAD;

    @Column(name = "fixed_file_path", length = 500)
    private String fixedFilePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "script_type", nullable = false, length = 30)
    private ImportScriptType scriptType = ImportScriptType.NONE;

    @Column(name = "script_path", length = 500)
    private String scriptPath;

    @Column(name = "script_args", length = 1000)
    private String scriptArgs;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_mode", nullable = false, length = 30)
    private ImportMode importMode = ImportMode.INSERT_ONLY;

    @Column(name = "header_row_number", nullable = false)
    private Integer headerRowNumber = 1;

    @Column(name = "data_start_row_number", nullable = false)
    private Integer dataStartRowNumber = 2;

    @Column(name = "charset", nullable = false, length = 50)
    private String charset = "UTF-8";

    @Column(name = "delimiter", nullable = false, length = 10)
    private String delimiter = ",";

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;

    @OneToMany(mappedBy = "target", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<ImportColumn> columns = new ArrayList<>();

    public void addColumn(ImportColumn column) {
        column.setTarget(this);
        this.columns.add(column);
    }

    public void clearColumns() {
        this.columns.clear();
    }
}