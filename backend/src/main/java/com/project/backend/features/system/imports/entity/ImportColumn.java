package com.project.backend.features.system.imports.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.imports.enums.ImportDataType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "import_column")
@Getter
@Setter
public class ImportColumn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private ImportTarget target;

    @Column(name = "column_name", nullable = false, length = 200)
    private String columnName;

    @Column(name = "csv_header_name", nullable = false, length = 200)
    private String csvHeaderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private ImportDataType dataType = ImportDataType.STRING;

    @Column(name = "required_flag", nullable = false)
    private boolean requiredFlag = false;

    @Column(name = "key_flag", nullable = false)
    private boolean keyFlag = false;

    @Column(name = "nullable_flag", nullable = false)
    private boolean nullableFlag = true;

    @Column(name = "trim_flag", nullable = false)
    private boolean trimFlag = true;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "format_pattern", length = 100)
    private String formatPattern;

    @Column(name = "updatable_flag", nullable = false)
    private boolean updatableFlag = true;

    @Column(name = "order_no", nullable = false)
    private int orderNo;
}