package com.project.backend.features.system.batch.entity;

import java.time.Instant;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.batch.enums.BatchJobType;
import com.project.backend.features.system.batch.enums.BatchScheduleType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "batch_job_definition")
@Getter
@Setter
public class BatchJobDefinition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_code", nullable = false, unique = true, length = 100)
    private String jobCode;

    @Column(name = "job_name", nullable = false, length = 200)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private BatchJobType jobType;

    @Column(name = "target_code", nullable = false, length = 100)
    private String targetCode;

    @Column(name = "immediate_executable", nullable = false)
    private boolean immediateExecutable = true;

    @Column(name = "schedule_enabled", nullable = false)
    private boolean scheduleEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 30)
    private BatchScheduleType scheduleType = BatchScheduleType.NONE;

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;

    @Column(name = "next_execute_at")
    private Instant nextExecuteAt;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;

    @Column(name = "description", length = 500)
    private String description;
}