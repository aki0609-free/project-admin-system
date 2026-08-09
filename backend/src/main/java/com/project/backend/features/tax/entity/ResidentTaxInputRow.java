package com.project.backend.features.tax.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "resident_tax_input_row", uniqueConstraints = @UniqueConstraint(
        name = "uk_resident_tax_input_row",
        columnNames = {"tenant_id", "batch_id", "employee_id", "month"}),
        indexes = @Index(name = "idx_resident_tax_input_row_batch", columnList = "tenant_id,batch_id"))
@Getter
@Setter
public class ResidentTaxInputRow extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "tax_amount")
    private Integer taxAmount;

    @Column(name = "current_tax_amount")
    private Integer currentTaxAmount;
}
