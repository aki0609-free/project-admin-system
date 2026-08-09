package com.project.backend.features.tax.entity;

import java.time.Instant;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.tax.enums.ResidentTaxInputStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "resident_tax_input_batch", indexes = @Index(
        name = "idx_resident_tax_input_batch_year_status",
        columnList = "tenant_id,fiscal_year,status"))
@Getter
@Setter
public class ResidentTaxInputBatch extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType = "MANUAL";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ResidentTaxInputStatus status = ResidentTaxInputStatus.DRAFT;

    @Column(name = "input_by", nullable = false, length = 100)
    private String inputBy;

    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "change_reason", length = 500)
    private String changeReason;
}
