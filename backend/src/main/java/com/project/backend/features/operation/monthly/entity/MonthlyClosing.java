package com.project.backend.features.operation.monthly.entity;

import java.time.Instant;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "monthly_closings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "target_month"})
        }
)
@Getter
@Setter
public class MonthlyClosing extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_month", nullable = false)
    private LocalDate targetMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MonthlyClosingStatus status = MonthlyClosingStatus.OPEN;

    @Column(name = "closing_version", nullable = false)
    private Integer closingVersion = 0;

    @Column(name = "closing_start_date")
    private LocalDate closingStartDate;

    @Column(name = "closing_end_date")
    private LocalDate closingEndDate;

    @Column(name = "closing_rule_type", length = 30)
    private String closingRuleType;

    @Column(name = "closing_rule_value")
    private Integer closingRuleValue;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    @Column(name = "note", length = 1000)
    private String note;
}