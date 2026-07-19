package com.project.backend.features.system.batch.entity;

import java.time.Instant;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.system.batch.enums.BatchExecutionStatus;
import com.project.backend.features.system.batch.enums.BatchJobType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "batch_execution_log")
@Getter
@Setter
public class BatchExecutionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_code", nullable = false, length = 100)
    private String jobCode;

    @Column(name = "job_name", nullable = false, length = 200)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private BatchJobType jobType;

    @Column(name = "target_code", nullable = false, length = 100)
    private String targetCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BatchExecutionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", length = 30)
    private StorageType storageType;

    @Column(name = "output_file_key", length = 1000)
    private String outputFileKey;

    @Column(name = "output_file_name", length = 255)
    private String outputFileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;
}