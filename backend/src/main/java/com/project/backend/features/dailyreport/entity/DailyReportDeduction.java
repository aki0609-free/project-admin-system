package com.project.backend.features.dailyreport.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_report_deductions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyReportDeduction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "daily_report_id", nullable = false)
    private Long dailyReportId;

    @Column(name = "deduction_master_id", nullable = false)
    private Long deductionMasterId;

    @Column(name = "deduction_code", nullable = false)
    private String deductionCode;

    @Column(name = "deduction_name", nullable = false)
    private String deductionName;

    @Column(name = "amount", nullable = false)
    private Integer amount;
}