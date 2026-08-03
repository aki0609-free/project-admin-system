package com.project.backend.features.system.excelbook.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "excel_book_data_source_catalog_column",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"tenant_id", "catalog_id", "column_name"}
        )
)
@Getter
@Setter
public class ExcelBookDataSourceCatalogColumn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_id", nullable = false)
    private ExcelBookDataSourceCatalog catalog;

    @Column(name = "column_name", nullable = false, length = 200)
    private String columnName;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "data_type", nullable = false, length = 30)
    private String dataType = "STRING";

    @Column(name = "order_no", nullable = false)
    private Integer orderNo = 1;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}
