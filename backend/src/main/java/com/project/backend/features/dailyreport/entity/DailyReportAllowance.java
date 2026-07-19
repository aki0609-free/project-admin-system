package com.project.backend.features.dailyreport.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_report_allowances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyReportAllowance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "daily_report_id", nullable = false)
    private Long dailyReportId;

    @Column(name = "allowance_master_id", nullable = false)
    private Long allowanceMasterId;

    @Column(name = "allowance_code", nullable = false)
    private String allowanceCode;

    @Column(name = "allowance_name", nullable = false)
    private String allowanceName;

    @Column(name = "amount", nullable = false)
    private Integer amount;
}