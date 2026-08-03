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
        name = "excel_book_variable_mapping",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"tenant_id", "master_id", "variable_key"}
        )
)
@Getter
@Setter
public class ExcelBookVariableMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_id", nullable = false)
    private ExcelBookMaster master;

    @Column(name = "variable_key", nullable = false, length = 100)
    private String variableKey;

    @Column(name = "source_column", nullable = false, length = 200)
    private String sourceColumn;

    @Column(name = "scope", nullable = false, length = 20)
    private String scope = "ROW";

    @Column(name = "data_type", nullable = false, length = 30)
    private String dataType = "STRING";

    @Column(name = "order_no", nullable = false)
    private Integer orderNo = 1;
}
