package com.project.backend.features.operation.monthly.entity;

import java.time.Instant;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "monthly_report_file_history")
@Getter
@Setter
public class MonthlyReportFileHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_month", nullable = false, length = 7)
    private String targetMonth;

    @Column(name = "history_version", nullable = false)
    private Integer historyVersion;

    @Column(name = "report_code", nullable = false, length = 100)
    private String reportCode;

    @Column(name = "report_name", nullable = false, length = 255)
    private String reportName;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "generated_by", length = 100)
    private String generatedBy;
}