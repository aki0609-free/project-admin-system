package com.project.backend.features.system.report.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.system.report.enums.ReportHistoryStatus;
import com.project.backend.features.system.report.enums.ReportOutputFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "report_history")
@Getter
@Setter
public class ReportHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_master_id", nullable = false)
    private ReportMaster reportMaster;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", length = 20)
    private ReportOutputFormat outputFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ReportHistoryStatus status = ReportHistoryStatus.SUCCESS;

    @Lob
    @Column(name = "request_params_json")
    private String requestParamsJson;

    @Column(name = "executed_by", length = 100)
    private String executedBy;

    @Lob
    @Column(name = "notes", columnDefinition = "LONGTEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", length = 20)
    private StorageType storageType;

    @Column(name = "stored_file_key", length = 1000)
    private String storedFileKey;

    @Column(name = "stored_file_name", length = 255)
    private String storedFileName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;
}