package com.project.backend.features.operation.preparation.entity;

import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.operation.preparation.enums.DailyPreparationStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "daily_preparations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "target_date"})
        }
)
@Getter
@Setter
public class DailyPreparation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DailyPreparationStatus status = DailyPreparationStatus.OPEN;

    @Column(name = "note", length = 1000)
    private String note;
}