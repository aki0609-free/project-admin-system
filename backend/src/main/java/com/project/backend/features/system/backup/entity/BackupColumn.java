package com.project.backend.features.system.backup.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.backup.enums.BackupDataType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "backup_column",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_backup_column_target_column",
                        columnNames = {"target_id", "column_name"}
                ),
                @UniqueConstraint(
                        name = "uk_backup_column_target_order",
                        columnNames = {"target_id", "order_no"}
                )
        }
)
@Getter
@Setter
public class BackupColumn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private BackupTarget target;

    @Column(name = "column_name", nullable = false, length = 200)
    private String columnName;

    @Column(name = "csv_header_name", nullable = false, length = 200)
    private String csvHeaderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private BackupDataType dataType;

    @Column(name = "export_flag", nullable = false)
    private boolean exportFlag = true;

    @Column(name = "order_no", nullable = false)
    private int orderNo;
}
