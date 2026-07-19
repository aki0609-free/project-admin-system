package com.project.backend.features.system.imports.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "import_error_row",
        indexes = {
                @Index(name = "idx_import_error_row_history", columnList = "history_id"),
                @Index(name = "idx_import_error_row_row_no", columnList = "row_no")
        }
)
@Getter
@Setter
public class ImportErrorRow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "history_id", nullable = false)
    private ImportHistory history;

    @Column(name = "row_no", nullable = false)
    private int rowNo;

    @Column(name = "csv_header_name", length = 200)
    private String csvHeaderName;

    @Column(name = "column_name", length = 200)
    private String columnName;

    @Column(name = "raw_value", length = 1000)
    private String rawValue;

    @Column(name = "error_message", nullable = false, length = 4000)
    private String errorMessage;
}