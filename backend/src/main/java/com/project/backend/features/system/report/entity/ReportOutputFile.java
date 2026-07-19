package com.project.backend.features.system.report.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.report.enums.ReportOutputFileStatus;
import com.project.backend.app.storage.enums.StorageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "report_output_file",
        indexes = {
                @Index(name = "idx_report_output_execution", columnList = "execution_id"),
                @Index(name = "idx_report_output_business_key", columnList = "business_key"),
                @Index(name = "idx_report_output_recipient", columnList = "recipient_key"),
                @Index(name = "idx_report_output_mail_type", columnList = "mail_type"),
                @Index(name = "idx_report_output_mail_template", columnList = "mail_template_key"),
                @Index(name = "idx_report_output_recipient_group", columnList = "recipient_group_key")
        }
)
@Getter
@Setter
public class ReportOutputFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false, length = 100)
    private String executionId;

    @Column(name = "report_code", nullable = false, length = 100)
    private String reportCode;

    @Column(name = "business_key", length = 255)
    private String businessKey;

    @Column(name = "recipient_key", length = 255)
    private String recipientKey;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(name = "recipient_group_key", length = 100)
    private String recipientGroupKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 20)
    private StorageType storageType = StorageType.LOCAL;

    @Column(name = "file_key", nullable = false, length = 1000)
    private String fileKey;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mail_type", length = 100)
    private String mailType;

    @Column(name = "mail_template_key", length = 100)
    private String mailTemplateKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReportOutputFileStatus status = ReportOutputFileStatus.CREATED;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;
}
