package com.project.backend.features.operation.monthly.entity;

import java.time.Instant;

import com.project.backend.app.base.entity.BaseEntity;
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
        name = "monthly_closing_report_files",
        indexes = {
                @Index(
                        name = "idx_monthly_closing_report_file_lookup",
                        columnList =
                                "tenant_id, target_month, report_code, "
                                        + "target_type, target_id, closing_version"
                ),
                @Index(
                        name = "idx_monthly_closing_report_file_closing",
                        columnList =
                                "tenant_id, monthly_closing_id, closing_version"
                )
        }
)
@Getter
@Setter
public class MonthlyClosingReportFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monthly_closing_id", nullable = false)
    private Long monthlyClosingId;

    @Column(name = "target_month", nullable = false, length = 7)
    private String targetMonth;

    @Column(name = "closing_version", nullable = false)
    private Integer closingVersion;

    @Column(name = "report_code", nullable = false, length = 100)
    private String reportCode;

    @Column(name = "job_code", nullable = false, length = 100)
    private String jobCode;

    /**
     * 帳票の出力対象種別。
     *
     * 例:
     * CUSTOMER
     * EMPLOYEE
     * CUSTOMER_SITE
     * ALL
     */
    @Column(name = "target_type", length = 50)
    private String targetType;

    /**
     * 帳票の出力対象ID。
     *
     * CUSTOMERならcustomerId、
     * EMPLOYEEならemployeeId、
     * CUSTOMER_SITEならcustomerSiteId。
     */
    @Column(name = "target_id")
    private Long targetId;

    /**
     * 一覧表示や検索時のスナップショット名。
     * 対象マスタの名称変更後も、出力時点の名称を保持する。
     */
    @Column(name = "target_name", length = 255)
    private String targetName;

    @Column(name = "batch_execution_log_id")
    private Long batchExecutionLogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", length = 20)
    private StorageType storageType;

    @Column(name = "output_file_key", length = 1000)
    private String outputFileKey;

    @Column(name = "output_file_name", length = 255)
    private String outputFileName;

    @Column(name = "content_type", length = 200)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "generated_at")
    private Instant generatedAt;
}